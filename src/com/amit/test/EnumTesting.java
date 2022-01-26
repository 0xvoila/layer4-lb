package com.amit.test;

enum Day{
    SUNDAY,
    MONDAY,
    TUESDAY,
    WEDNESDAY,
    THURSDAY,
    FRIDAY,
    SATURDAY
}

public class EnumTesting {

    Day dayOfTheWeek;

    public EnumTesting(Day day){
        this.dayOfTheWeek = day;
    }

    public void display(){
        System.out.println("Day of the week is " + this.dayOfTheWeek);
    }

    public void displayAll(){

        for(Day day : Day.values()){
            System.out.println(day);
        }
    }

    public static void main(String args[]){

        EnumTesting enumTesting = new EnumTesting(Day.MONDAY);
        enumTesting.display();
        enumTesting.displayAll();
    }
}