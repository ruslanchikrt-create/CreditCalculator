package com.example.creditcalculator;

/** Pure financial formulas. All rates are nominal annual percentages, e.g. 24 means 24% p.a. */
public final class FinanceMath {
    private static final double CENT_EPS = 0.005;
    private FinanceMath() {}

    public static final class Plan {
        public final int monthsUsed;
        public final double interest;
        public final double totalPaid;
        public final double finalPayment;
        public final double remainingBalance;
        Plan(int monthsUsed,double interest,double totalPaid,double finalPayment,double remainingBalance){
  this.monthsUsed=monthsUsed;this.interest=interest;this.totalPaid=totalPaid;this.finalPayment=finalPayment;this.remainingBalance=remainingBalance;
        }
    }

    private static double monthlyRate(double annualRatePercent){
        if(!Double.isFinite(annualRatePercent)||annualRatePercent<0)throw new IllegalArgumentException("rate");
        return annualRatePercent/100d/12d;
    }
    private static void principalMonths(double principal,int months){
        if(!Double.isFinite(principal)||principal<=0||months<=0)throw new IllegalArgumentException("principal/months");
    }

    public static double annuityPayment(double principal,int months,double annualRatePercent){
        principalMonths(principal,months);double m=monthlyRate(annualRatePercent);if(m==0)return principal/months;
        double factor=Math.pow(1d+m,months);return principal*m*factor/(factor-1d);
    }
    public static double annuityInterest(double principal,int months,double annualRatePercent){
        return Math.max(0,annuityPayment(principal,months,annualRatePercent)*months-principal);
    }
    public static double differentialFirstPayment(double principal,int months,double annualRatePercent){
        principalMonths(principal,months);double m=monthlyRate(annualRatePercent);return principal/months+principal*m;
    }
    public static double differentialLastPayment(double principal,int months,double annualRatePercent){
        principalMonths(principal,months);double part=principal/months;return part+part*monthlyRate(annualRatePercent);
    }
    public static double differentialTotalInterest(double principal,int months,double annualRatePercent){
        principalMonths(principal,months);double m=monthlyRate(annualRatePercent);return Math.max(0,principal*m*(months+1d)/2d);
    }

    /**
     * A fixed-payment plan with a contractual maximum term. If the regular payment does not
     * close the balance exactly, the last payment is adjusted to the exact balance + interest.
     * If the regular payment closes the debt earlier, the plan stops earlier.
     */
    public static Plan fixedPaymentPlan(double principal,int months,double annualRatePercent,double regularPayment){
        principalMonths(principal,months);if(!Double.isFinite(regularPayment)||regularPayment<=0)throw new IllegalArgumentException("payment");
        double m=monthlyRate(annualRatePercent),balance=principal,totalInterest=0,totalPaid=0,finalPayment=0;int used=0;
        for(int i=0;i<months&&balance>CENT_EPS;i++){
  double interest=balance*m;double due=balance+interest;double payment;
  if(i==months-1)payment=due;
  else{
      if(regularPayment<=interest+1e-9)throw new IllegalArgumentException("payment does not amortize principal");
      payment=Math.min(regularPayment,due);
  }
  double principalPart=Math.max(0,payment-interest);
  totalInterest+=interest;totalPaid+=payment;finalPayment=payment;used=i+1;
  balance=Math.max(0,balance-principalPart);
        }
        return new Plan(used,Math.max(0,totalInterest),Math.max(0,totalPaid),Math.max(0,finalPayment),Math.max(0,balance));
    }

    public static int monthsToPayoff(double principal,double annualRatePercent,double regularPayment,int maxMonths){
        if(!Double.isFinite(principal)||principal<=0)return 0;if(maxMonths<=0||!Double.isFinite(regularPayment)||regularPayment<=0)return -1;
        double m=monthlyRate(annualRatePercent),balance=principal;
        for(int n=1;n<=maxMonths;n++){
  double interest=balance*m;if(regularPayment<=interest+1e-9)return -1;
  double due=balance+interest;double payment=Math.min(regularPayment,due);balance=Math.max(0,due-payment);
  if(balance<=CENT_EPS)return n;
        }
        return -1;
    }

    public static double fixedPaymentInterest(double principal,int months,double annualRatePercent,double regularPayment){
        return fixedPaymentPlan(principal,months,annualRatePercent,regularPayment).interest;
    }

    /** Differential plan where firstPayment determines the regular principal part; last month closes any remainder. */
    public static double differentialInterestWithFirstPayment(double principal,int months,double annualRatePercent,double firstPayment){
        principalMonths(principal,months);double m=monthlyRate(annualRatePercent),firstInterest=principal*m;
        double principalPart=firstPayment>firstInterest+CENT_EPS?firstPayment-firstInterest:principal/months;
        if(principalPart<=0)throw new IllegalArgumentException("payment");
        double balance=principal,total=0;
        for(int i=0;i<months&&balance>CENT_EPS;i++){
  total+=balance*m;
  if(i==months-1){balance=0;break;}
  balance=Math.max(0,balance-Math.min(principalPart,balance));
        }
        return Math.max(0,total);
    }

    public static double simpleDepositInterest(double principal,int months,double annualRatePercent){
        principalMonths(principal,months);return Math.max(0,principal*annualRatePercent/100d*(months/12d));
    }
    public static double monthlyCompoundDepositFinal(double principal,int months,double annualRatePercent){
        principalMonths(principal,months);double m=monthlyRate(annualRatePercent);return principal*Math.pow(1d+m,months);
    }
    public static double scheduledTotal(double principal,int months,double annualRatePercent,boolean differential){
        if(differential)return principal+differentialTotalInterest(principal,months,annualRatePercent);
        return annuityPayment(principal,months,annualRatePercent)*months;
    }

    public static double financedCosts(double currentBalance,double newPrincipal,double fees){
        return Math.max(0,Math.min(Math.max(0,fees),Math.max(0,newPrincipal-currentBalance)));
    }
    public static double cashOut(double currentBalance,double newPrincipal,double fees){
        return Math.max(0,Math.max(0,newPrincipal-currentBalance)-financedCosts(currentBalance,newPrincipal,fees));
    }
    public static double outOfPocketCosts(double currentBalance,double newPrincipal,double fees){
        return Math.max(0,Math.max(0,fees)-financedCosts(currentBalance,newPrincipal,fees));
    }
    /** Comparable future cash cost, excluding cash-out principal because that is money received by the borrower. */
    public static double comparableRefinanceFutureCost(double currentBalance,double newPrincipal,double fees,double newScheduledTotal){
        return Math.max(0,newScheduledTotal-cashOut(currentBalance,newPrincipal,fees)+outOfPocketCosts(currentBalance,newPrincipal,fees));
    }
}
