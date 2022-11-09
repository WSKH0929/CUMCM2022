package com.wskh;

import lombok.Data;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @Author：WSKH
 * @ClassName：Run
 * @ClassType：
 * @Description：
 * @Date：2022/9/16/21:54
 * @Email：1187560563@qq.com
 * @Blog：https://blog.csdn.net/weixin_51545953?type=blog
 */
public class Run {
    public static void main(String[] args) throws Exception {
        run("高钾");
        System.out.println("==================================================================");
        run("铅钡");
    }

    public static void run(String type) throws Exception {
        String path = "D:\\2 BachelorDegree\\大四上比赛或项目资料\\数模国赛\\OurGit\\cumcm2022\\05 Wskh\\Python\\CUMCM2022\\src\\data\\data.xlsx";
        List<List<Double>> dataList = readData(path,type);
        System.out.println(dataList);
        ISODATA isodata = new ISODATA(
                dataList,
                5,
                2,
                2,
                0.5,
                2,
                4000);
        List<List<Double>> clustering = isodata.clustering();
        Map<Integer, List<List<Double>>> map = isodata.getMap();
        StringBuilder label = new StringBuilder();
        StringBuilder data = new StringBuilder();
        for (List<Double> doubleList : dataList) {
            boolean b = true;
            for (Integer key : map.keySet()) {
                for (List<Double> list : map.get(key)) {
                    if(list.toString().equals(doubleList.toString())){
                        data.append(list.toString()).append(",");
                        label.append(key).append(",");
                        b = false;
                        break;
                    }
                }
                if(!b){
                    break;
                }
            }
        }
        System.out.println("label = "+label);
        System.out.println("data = "+data);
    }

    public static List<List<Double>> readData(String path,String type) {
        try {
            XSSFWorkbook xssfWorkbook = new XSSFWorkbook(path);
            XSSFSheet sheet = xssfWorkbook.getSheetAt(0);
            List<List<Double>> dataList = new ArrayList<>();
            for (int i = 1; i < sheet.getPhysicalNumberOfRows(); i++) {
                if(type.equals(sheet.getRow(i).getCell(18).getStringCellValue())){
                    List<Double> list = new ArrayList<>();
                    for (int j = 1; j <= 14; j++) {
                        list.add(sheet.getRow(i).getCell(j).getNumericCellValue());
                    }
                    dataList.add(list);
                }
            }

            xssfWorkbook.close();
            return dataList;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
