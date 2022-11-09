import numpy as np
from sklearn import tree
from sklearn.datasets import load_wine #红酒数据集
from sklearn.model_selection import train_test_split
import pandas as pd
import graphviz
import matplotlib.pyplot as plt

#DecisionTreeClassifier分类树代码

wine = load_wine() #加载红酒数据集
print(wine)
#wine.data 数据
#wine.target 分类标签
#wine.feature_names 要素名称
#wine.target_name  分类标签的名字含义

#我们可以通过pandas把红酒数据集变成表格，方便我们查看
table = pd.concat([pd.DataFrame(wine.data),pd.DataFrame(wine.target)],axis=1)
print(table)

#把wine数据分为训练集和测试集
Xtrain,Xtest,Ytrain,Ytest = train_test_split(wine.data, #数据
                                             wine.target,#标签
                                             test_size=0.3)#经典的三七分，测试集占三，训练集占七

#设置参数
clf = tree.DecisionTreeClassifier(criterion="entropy"    #用来规定不纯度的计算，可以输入的参数有"entropy"：信息熵;"gini":基尼系数
                                  ,random_state=0        #用来控制决策树的随机性
                                  ,splitter='random'     #也是用来控制决策树的随机性，
                                  # 可以输入的参数有"best":优先取对决策贡献率最高的特征进行分支;"random":随机分支
                                  ,max_depth=3           #控制决策树的最大深度
                                  ,min_samples_leaf=10   #控制叶子的最小尺寸，即叶子最少包含的样本量
                                  ,min_samples_split=10  #控制节点的最小尺寸，即节点最少包含的样本量
                                  ,max_features= 8       #限制分支时考虑的特征个数
                                  ,min_impurity_decrease=0.01  #限制信息增益的大小，即限制子节点和父节点信息熵之差的大小
                                  )
#开始训练
clf = clf.fit(Xtrain,Ytrain)
#评估得分
score = clf.score(Xtest,Ytest)
print('决策树评估得分为:',score)

# list = [[1,2,3,4,5,6,7,8,9,10,11,12,13]]
# print(clf.predict(np.array(list)))
# print(clf.predict_proba(np.array(list)))

#把树给画出来
#定义要素名称
feature_name = ['酒精','苹果酸','灰','灰的碱性','镁','总酚','类黄酮','非黄烷类酚类','花青素',
                '颜色强度','色调','od280/od315稀释葡萄酒','脯氨酸']
dot_data = tree.export_graphviz(clf,  #已经训练好的模型，也就是之前训练时定义的clf
                                feature_names=feature_name,  #传入上方定义的要素名称
                                class_names=['啤酒','红酒','白酒'], #酒的分类
                                filled=True, #是否给输出的图案填充颜色
                              #  rounded=True #是否给方框加上圆角
                                )
#把画的树导出来
# graph = graphviz.Source(dot_data)
# graph.view(cleanup=True)  #这个参数可以让生成新文件直接先清除掉旧的文件，cleanup默认为False

#clf.feature_importances_这个属性可以查看决策树用到了哪些特征
print(clf.feature_importances_)

print([*zip(feature_name,clf.feature_importances_)]) #可以把特征名字，和特征对决策的贡献率打包在一起显示，方便查看

#通过for循环多次调参数并计算得分和plot画图，来确定最佳的参数值
test = []
for i in range(10):
    clf = tree.DecisionTreeClassifier(criterion='gini'
                                      ,max_depth=i+1
                                      )
    clf=clf.fit(Xtrain,Ytrain)
    score = clf.score(Xtest,Ytest)
    test.append(score)
#开始画图
plt.plot(range(1,11),test,'r-',label="max_depth")
plt.legend()
plt.show()

#重要的属性和接口
clf.apply(Xtest)   #返回每个测试样本所在叶节点的索引
clf.predict(Xtest) #返回每个测试样本的分类/回归结果