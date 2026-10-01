package util;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

public final class DepositPolicy {
    public static final double DEPOSIT_RATE_PERCENT = 50.0;

    private DepositPolicy() {
    }

    public static int calculateNights(LocalDateTime checkIn, LocalDateTime checkOut) {
        if (checkIn == null || checkOut == null || !checkOut.isAfter(checkIn)) {
            return 0;
        }
        long nights = ChronoUnit.DAYS.between(checkIn.toLocalDate(), checkOut.toLocalDate());
        return (int) Math.max(1, nights);
    }

    public static double calculateDeposit(double totalRoomAmount) {
        return roundMoney(totalRoomAmount * DEPOSIT_RATE_PERCENT / 100.0);
    }

    public static double calculateRemaining(double totalRoomAmount, double depositAmount) {
        return roundMoney(Math.max(0, totalRoomAmount - depositAmount));
    }

    public static double calculateReceivedDeposit(double totalRoomAmount, double customerPaidAmount) {
        return roundMoney(Math.max(0, Math.min(customerPaidAmount, totalRoomAmount)));
    }

    public static double calculateChange(double totalRoomAmount, double customerPaidAmount) {
        return roundMoney(Math.max(0, customerPaidAmount - totalRoomAmount));
    }

    public static int daysSinceDeposit(LocalDateTime depositAt, LocalDateTime cancelAt) {
        if (depositAt == null || cancelAt == null || cancelAt.isBefore(depositAt)) {
            return 0;
        }
        long days = ChronoUnit.DAYS.between(depositAt.toLocalDate(), cancelAt.toLocalDate());
        return (int) Math.max(0, days);
    }

    public static double refundRatePercent(int daysSinceDeposit) {
        // Current policy is based on days elapsed after deposit. Change this method
        // if the hotel later switches to days before check-in.
        if (daysSinceDeposit <= 0) {
            return 100.0;
        }
        if (daysSinceDeposit <= 2) {
            return 75.0;
        }
        if (daysSinceDeposit < 7) {
            return 50.0;
        }
        return 0.0;
    }

    public static double calculateRefund(double depositAmount, double refundRatePercent) {
        return roundMoney(depositAmount * refundRatePercent / 100.0);
    }

    public static double roundMoney(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
