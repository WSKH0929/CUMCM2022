package com.wskh;

import lombok.Data;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/**
 * @Author：WSKH
 * @ClassName：ISODATA
 * @ClassType：
 * @Description：
 * @Date：2022/2/13/14:23
 * @Email：1187560563@qq.com
 * @Blog：https://blog.csdn.net/weixin_51545953?type=blog
 */
@Data
public class ISODATA {

    // 待分类的原始值
    private List<List<Double>> dataList;
    // 初始类别个数
    private int K = 4;
    // 最大迭代次数
    private int maxClusterTimes = 2000;
    // 聚类的结果
    private List<List<List<Double>>> clusterList = new ArrayList<>();
    // <种群索引,点集>
    Map<Integer, List<List<Double>>> map;
    // 每一聚类域中最少的样本数目，若少于此数即不作为一个独立的聚类
    int minPopulationPointNum;
    // 两个聚类中心间的最小距离，若小于此数，两个聚类需进行合并
    double centerMinDistance;
    // 在一次迭代运算中可以合并的聚类中心的最多对数
    int maxCombineNum;
    // 一个聚类域中样本距离分布的标准差
    double maxMAE;

    /**
     * @param dataList 数据集
     * @param centerMinDistance 两个聚类中心间的最小距离，若小于此数，两个聚类需进行合并
     * @param minPopulationPointNum 每一聚类域中最少的样本数目，若少于此数即不作为一个独立的聚类；
     * @param maxCombineNum 在一次迭代运算中可以合并的聚类中心的最多对数
     * @param K 初始类别个数
     * @param maxClusterTimes 最大迭代次数
     * @return
     * @Description
     * @Author WSKH
     */
    public ISODATA(List<List<Double>> dataList, double centerMinDistance, int minPopulationPointNum, int maxCombineNum, double maxMAE, int K, int maxClusterTimes) {
        this.dataList = new ArrayList<>(dataList);
        this.K = Math.min(K, dataList.size());
        this.maxClusterTimes = maxClusterTimes;
        this.centerMinDistance = centerMinDistance;
        this.minPopulationPointNum = minPopulationPointNum;
        this.maxCombineNum = maxCombineNum;
        this.maxMAE = maxMAE;
    }

    /**
     * @param
     * @return 聚类的结果(簇心)
     * @Description 聚类主方法
     * @Author WSKH
     */
    public List<List<Double>> clustering() throws Exception {
        long start = System.currentTimeMillis();
        int t = 0;
        while (t < maxClusterTimes) {
            if (t == 0) {
                // 初始化簇心
                clusterList.add(initCenterList(K));
            } else {
                // 获取当前簇心
                List<List<Double>> centerList = clusterList.get(t - 1);
                // 计算新的簇心
                map = new HashMap<>();
                int[] counterArr = new int[centerList.size()];
                for (List<Double> data : dataList) {
                    // 当前点和簇心依次比较，找到最近的簇心
                    double minDis = computeDistance(data, centerList.get(0));
                    int minIndex = 0;
                    for (int i = 1; i < centerList.size(); i++) {
                        double distance = computeDistance(data, centerList.get(i));
                        if (minDis > distance) {
                            minDis = distance;
                            minIndex = i;
                        }
                    }
                    // 将当前点加入最近的种群
                    if (!map.containsKey(minIndex)) {
                        List<List<Double>> newPointList = new ArrayList<>();
                        newPointList.add(data);
                        map.put(minIndex, newPointList);
                    } else {
                        map.get(minIndex).add(data);
                    }
                    counterArr[minIndex]++;
                }
                // 根据均值，计算新的簇心
                List<List<Double>> newCenterList = new ArrayList<>();
                for (int i = 0; i < centerList.size(); i++) {
                    if (map.containsKey(i)) {
                        // 计算簇心
                        List<Double> newCenter = computeCenter(map.get(i));
                        newCenterList.add(newCenter);
                    } else {
                        throw new RuntimeException("发生了簇缺失");
                    }
                }
                // 适应性分裂与合并簇心
                newCenterList = adaptiveSplittingAndMerging(newCenterList);
                // 将新的簇心，加入集合
                clusterList.add(newCenterList);
            }
            // 如果簇心没有改变，那么就跳出循环
            if (t > 0 && !isCenterChange(clusterList.get(t - 1), clusterList.get(t))) {
                break;
            }
            t++;
        }
        System.out.println("用时" + (System.currentTimeMillis() - start) + "ms");
        return clusterList.get(clusterList.size() - 1);
    }

    /**
     * @param centerList 簇心集合
     * @return
     * @Description 适应性分裂与合并簇心
     * @Author WSKH
     */
    private List<List<Double>> adaptiveSplittingAndMerging(List<List<Double>> centerList) throws Exception {
        int c1 = centerList.size();
        List<List<Double>> newCenterList = new ArrayList<>();
        // 适应性分裂
        map.forEach((k,v)->{
            // 中心点
            List<Double> center = centerList.get(k);
            // 计算SSE 然后推导出MAE
            AtomicReference<Double> SSE = new AtomicReference<>(0d);
            v.forEach(point->{
                for (int i = 0; i < point.size(); i++) {
                    int finalI = i;
                    SSE.updateAndGet(v1 -> (v1 + Math.pow(point.get(finalI) - center.get(finalI), 2)));
                }
            });
            if(Math.sqrt(SSE.get()/v.size())>maxMAE){
                // 说明可以分裂，找到一个点距离中心点超过
                for (int i = 0; i < dataList.size(); i++) {
                    try {
                        if(computeDistance(dataList.get(i),center)>centerMinDistance){
                            newCenterList.add(dataList.get(i));
                            break;
                        }else{
                            if(i==dataList.size()-1){
                                // 如果没找到那就报错，说明又要分裂，分裂后又要合并，参数设置不合理
                                throw new Exception("参数centerMinDistance和maxMAE设置不合理");
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

            }
        });
        // 适应性合并
        newCenterList.addAll(centerList);
        int c2 = newCenterList.size();
        int counter = 0; // 记录合并次数
        for (int i = 0; i < newCenterList.size(); i++) {
            if(counter > maxCombineNum){
                break;
            }
            for (int j = i+1; j < newCenterList.size(); j++) {
                if(computeDistance(newCenterList.get(i),newCenterList.get(j))<=centerMinDistance){
                    // 小于最小距离，合并
                    List<Double> remove1 = newCenterList.get(i);
                    List<Double> remove2 = newCenterList.get(j);
                    // 新中心就是合并点的中点
                    List<Double> newCenter = new ArrayList<>();
                    for (int k = 0; k < remove1.size(); k++) {
                        newCenter.add((remove1.get(k)+remove2.get(k))/2.0);
                    }
                    if(i>j){
                        newCenterList.remove(i);
                        newCenterList.remove(j);
                    }else{
                        newCenterList.remove(i);
                        newCenterList.remove(j-1);
                    }
                    newCenterList.add(newCenter);
                    i = -1; // 让i从头开始遍历
                    counter++;
                    break;
                }
            }
        }
        // 返回分裂与合并后的中心点集合
        System.out.println("一开始:"+c1+",分裂后："+c2+",合并后:"+newCenterList.size());
        return newCenterList;
    }

    /**
     * @param size 初始化簇心的数量
     * @return List<List < Double>>
     * @Description 初始化簇心(随机抽取size项作为簇心，尽可能使得初始簇心相互距离较远)
     * @Author WSKH
     */
    private List<List<Double>> initCenterList(int size) throws Exception {
        List<List<Double>> initCenterList = new ArrayList<>();
        Set<Integer> set = new HashSet<>();
        Double[][] distanceMatrix = new Double[dataList.size()][dataList.size()];
        // 随机选取第一个簇心
        int r = new Random().nextInt(dataList.size());
        set.add(r);
        // 选择出其余的聚类中心
        while (set.size()<size){
            double maxDistance = -1d;
            int maxIndex = -1;
            for (int i = 0; i < dataList.size(); i++) {
                List<Double> data = dataList.get(i);
                if(!set.contains(i)){
                    // 计算当前点，距离最近的已有簇心
                    double minDistance = Double.MAX_VALUE;
                    int minIndex = -1;
                    for (Integer j : set) {
                        if(distanceMatrix[i][j]==null){
                            distanceMatrix[i][j] = computeDistance(data,dataList.get(j));
                        }
                        if(minDistance>distanceMatrix[i][j]){
                            minDistance = distanceMatrix[i][j];
                            minIndex = i;
                        }
                    }
                    // 获取最小距离中最大的那个(最大化点与簇心的最短距离)
                    if(maxDistance<minDistance){
                        maxDistance = minDistance;
                        maxIndex = minIndex;
                    }
                }
            }
            set.add(maxIndex);
        }
        // set -> list
        set.forEach(i->initCenterList.add(dataList.get(i)));
        return initCenterList;
    }

    /**
     * @param p1 点1
     * @param p2 点2
     * @return double
     * @Description 计算两点间距离（欧式）
     * @Author WSKH
     */
    private double computeDistance(List<Double> p1, List<Double> p2) throws Exception {
        if (p1.size() != p2.size()) {
            throw new Exception("两点维度不一致");
        }
        double distance = 0d;
        for (int i = 0; i < p1.size(); i++) {
            distance += Math.pow((p1.get(i) - p2.get(i)), 2);
        }
        return Math.sqrt(distance);
    }

    /**
     * @param pointList 点集合
     * @return double
     * @Description 计算簇心坐标
     * @Author WSKH
     */
    private List<Double> computeCenter(List<List<Double>> pointList) {
        List<Double> result = new ArrayList<>(pointList.get(0));
        for (int i = 1; i < pointList.size(); i++) {
            for (int j = 0; j < pointList.get(i).size(); j++) {
                result.set(j, result.get(j) + pointList.get(i).get(j));
            }
        }
        return result.stream().map(item -> {
            return item / pointList.size();
        }).collect(Collectors.toList());
    }

    /**
     * @param oldCenterList 上一轮迭代的簇心列表
     * @param curCenterList 当前迭代的簇心列表
     * @return boolean 改变返回true，没改变返回false
     * @Description 判断簇心是否改变
     * @Author WSKH
     */
    private boolean isCenterChange(List<List<Double>> oldCenterList, List<List<Double>> curCenterList) throws Exception {
        if (oldCenterList.size() != curCenterList.size()) {
            return true;
        }
        for (int i = 0; i < oldCenterList.size(); i++) {
            for (int j = 0; j < oldCenterList.get(i).size(); j++) {
                if (!oldCenterList.get(i).get(j).equals(curCenterList.get(i).get(j))) {
                    return true;
                }
            }
        }
        return false;
    }

}
