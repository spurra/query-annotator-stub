from sklearn.datasets import load_svmlight_file
from sklearn.datasets import dump_svmlight_file
from sklearn import preprocessing
import numpy as np
from pandas import DataFrame 
import scipy

X, y = load_svmlight_file("data/svm/train_dataset.txt")
X = X.todense()
print("=================")
print(X)
print(X.mean(axis=0))
print(X.std(axis=0))
print("=================")

np.savetxt('data/svm/mean.txt', X.mean(axis=0))
np.savetxt('data/svm/std.txt', X.std(axis=0))

X_scaled = preprocessing.scale(X, copy = False)

print("=================")
print(X_scaled)
print(X_scaled.mean(axis=0))
print(X_scaled.std(axis=0))
print("=================")

#y_test = [i[] for i in y]
dump_svmlight_file(X_scaled, y, "data/svm/train_dataset_scaled.txt", zero_based = False)

