# -*-coding:utf-8-*-
# Author: WSKH
# Blog: wskh0929.blog.csdn.net
# Time: 2022/9/15 21:50
import pandas as pd
import scipy
from sklearn.cluster import KMeans

data = pd.read_excel(
    r'D:\2 BachelorDegree\大四上比赛或项目资料\数模国赛\OurGit\cumcm2022\05 Wskh\Python\CUMCM2022\src\data\data.xlsx',
    sheet_name="Sheet1")
x = []
columns = data.columns
for i in range(data.shape[0]):
    # pass
    lst = []
    for item in data.iloc[i][columns[1:15]]:
        lst.append(item)
    x.append(lst)
print(x)
y_pred = KMeans(n_clusters=2, random_state=520).fit_predict(x)
print(y_pred)
