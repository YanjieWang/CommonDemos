#include <stdio.h>
#include <test2.h>
using namespace std;
void saybye(){
    printf("bye!\n");


    int p[100];
	
    for(int j=0;j<100;j++){
   		p[j]=j*j;
        cout<<p[j]<<":"<<&p[j]<<endl;
        int *q=(int *)&p[j];
        q=(int*)0x7fff72a091cc;
        cout<<*q<<":"<<q<<endl;
	}
    cout<<&"haha"<<endl;
    cout<<&"haha"<<endl;
    char x[10];
    cin>>x;
	cout<<x<<endl;
}
