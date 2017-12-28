# Computer_Network_Virtual_Routing

## 1.0

1. 将ServerThread里面的While（true）删除，因为java中的新建线程后的参数其实是原来的副本，如果在外面修改，以前建立的线程的参数是不会改变的。
2. 能够实现三台主机连成一条直线A->B->C,且A可以给C发送信息