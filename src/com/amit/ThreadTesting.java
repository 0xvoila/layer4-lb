//package com.amit;
//
//import java.util.concurrent.locks.Condition;
//import java.util.concurrent.locks.ReentrantLock;
//import java.util.concurrent.locks.ReentrantReadWriteLock;
//
//public class ThreadTesting extends Thread {
//
//    public static void main(String args[]){
//
//        Queue queue = new Queue();
//        new Thread(new ELB(queue)).start();
//        new Thread(queue).start();
//    }
//}
//
//class ELB extends Thread{
//
//    Queue queue;
//
//    ELB(Queue queue){
//        this.queue = queue;
//    }
//    public void run(){
//        System.out.println(this.queue.getValue());
//    }
//}
//
//class Queue extends Thread{
//
//    static int x;
//    ReentrantLock reentrantLock = new ReentrantLock();
//    Condition valueIsSetup = reentrantLock.newCondition();
//
//    public void run(){
//        try{
//            reentrantLock.lock();
//            x = 100;
//            sleep(1000);
//            valueIsSetup.signalAll();
//
//        }
//        catch(Exception exception){
//               exception.printStackTrace();
//        }
//        finally {
//            reentrantLock.unlock();
//            System.out.println("I am done with setting up the value");
//        }
//
//    }
//
//    public int getValue() {
//        try{
//            reentrantLock.lock();
//
//            while(x == 0){
//                System.out.println("I am going in waiting state, idle");
//                valueIsSetup.await();
//            }
//
//            System.out.println(" I have woken up");
//        }
//        catch(Exception exception){
//            exception.printStackTrace();
//        }
//        finally {
//            reentrantLock.unlock();
//            return x;
//        }
//    }
//}
