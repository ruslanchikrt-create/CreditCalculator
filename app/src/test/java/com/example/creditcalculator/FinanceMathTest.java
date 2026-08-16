package com.example.creditcalculator;

import org.junit.Test;
import static org.junit.Assert.*;

public class FinanceMathTest {
    private static void near(double expected,double actual,double tolerance){assertEquals(expected,actual,tolerance);}

    @Test public void annuityKnownCase(){
        near(12505.9593225,FinanceMath.annuityPayment(300000,33,24),0.01);
        near(112696.657643,FinanceMath.annuityInterest(300000,33,24),0.02);
    }
    @Test public void zeroRate(){near(6944.444444,FinanceMath.annuityPayment(250000,36,0),0.001);}
    @Test public void differentialKnownCase(){
        near(15090.909091,FinanceMath.differentialFirstPayment(300000,33,24),0.01);
        near(9272.727273,FinanceMath.differentialLastPayment(300000,33,24),0.01);
        near(102000.0,FinanceMath.differentialTotalInterest(300000,33,24),0.01);
    }
    @Test public void fixedPlanAdjustsLastPaymentInsteadOfMultiplyingRegularPayment(){
        FinanceMath.Plan p=FinanceMath.fixedPaymentPlan(209841.41,30,24,9539.18);
        assertEquals(30,p.monthsUsed);near(69446.449036,p.interest,0.02);near(2651.639036,p.finalPayment,0.02);near(0,p.remainingBalance,0.01);
    }
    @Test public void reduceTermSavesMoreThanReducePaymentForStandardAnnuity(){
        double balance=219841.41,prepay=10000,rate=24,currentPayment=9539.18;int remaining=32;
        double oldInterest=FinanceMath.fixedPaymentInterest(balance,remaining,rate,currentPayment);
        double newBalance=balance-prepay;
        double reducedPayment=FinanceMath.annuityPayment(newBalance,remaining,rate);
        double reducePaymentInterest=FinanceMath.fixedPaymentInterest(newBalance,remaining,rate,reducedPayment);
        int shorter=FinanceMath.monthsToPayoff(newBalance,rate,currentPayment,1200);
        double reduceTermInterest=FinanceMath.fixedPaymentInterest(newBalance,shorter,rate,currentPayment);
        assertEquals(30,shorter);
        near(1537.187129,oldInterest-reducePaymentInterest,0.03);
        near(8376.365666,oldInterest-reduceTermInterest,0.03);
        assertTrue(reduceTermInterest<reducePaymentInterest);
    }
    @Test public void longMortgageLargeSavingIsMathematicallyPossible(){
        double balance=2700635.48,rate=18,payment=40691.31,prepay=100000;
        int oldMonths=FinanceMath.monthsToPayoff(balance,rate,payment,1200);
        int newMonths=FinanceMath.monthsToPayoff(balance-prepay,rate,payment,1200);
        assertEquals(364,oldMonths);assertEquals(215,newMonths);
        double oldI=FinanceMath.fixedPaymentInterest(balance,oldMonths,rate,payment);
        double newI=FinanceMath.fixedPaymentInterest(balance-prepay,newMonths,rate,payment);
        assertTrue(oldI-newI>5_000_000);
    }
    @Test public void refinanceIncludesFeesAndDoesNotTreatCashOutPrincipalAsACost(){
        double balance=1_000_000,oldRate=20,newRate=15;int months=36;
        double oldFuture=FinanceMath.scheduledTotal(balance,months,oldRate,false);
        double newPrincipal=1_030_000,fees=30_000;
        double newScheduled=FinanceMath.scheduledTotal(newPrincipal,months,newRate,false);
        double comparable=FinanceMath.comparableRefinanceFutureCost(balance,newPrincipal,fees,newScheduled);
        near(52498.620015,oldFuture-comparable,0.05);
        double cashPrincipal=1_130_000;
        double cashScheduled=FinanceMath.scheduledTotal(cashPrincipal,months,newRate,false);
        near(100000,FinanceMath.cashOut(balance,cashPrincipal,fees),0.01);
        assertTrue(FinanceMath.comparableRefinanceFutureCost(balance,cashPrincipal,fees,cashScheduled)<cashScheduled);
    }
    @Test public void deposits(){
        near(24000,FinanceMath.simpleDepositInterest(100000,24,12),0.01);
        near(126973.464,FinanceMath.monthlyCompoundDepositFinal(100000,24,12),0.1);
    }
}
