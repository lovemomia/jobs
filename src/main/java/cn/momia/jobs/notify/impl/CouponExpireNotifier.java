package cn.momia.jobs.notify.impl;

import cn.momia.api.course.CouponServiceApi;
import cn.momia.api.course.dto.coupon.UserCoupon;
import cn.momia.api.im.ImServiceApi;
import cn.momia.jobs.notify.Notifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CouponExpireNotifier implements Notifier {
    private static final Logger LOGGER = LoggerFactory.getLogger(CouponExpireNotifier.class);

    @Autowired private CouponServiceApi couponServiceApi;
    @Autowired private ImServiceApi imServiceApi;

    @Override
    public void notifyUser() {
        List<UserCoupon> userCouponsToExpired = couponServiceApi.queryUserCouponsToExpired(3);
        if (userCouponsToExpired.isEmpty()) return;

        if (userCouponsToExpired.size() > 1000) {
            // FIXME 用户大了需要整个重构
            LOGGER.error("too many user coupons to expired");
            return;
        }

        Map<Long, List<UserCoupon>> userCouponsMap = new HashMap<Long, List<UserCoupon>>();
        for (UserCoupon userCoupon : userCouponsToExpired) {
            List<UserCoupon> userCoupons = userCouponsMap.get(userCoupon.getUserId());
            if (userCoupons == null) {
                userCoupons = new ArrayList<UserCoupon>();
                userCouponsMap.put(userCoupon.getUserId(), userCoupons);
            }
            userCoupons.add(userCoupon);
        }

        for (Map.Entry<Long, List<UserCoupon>> entry : userCouponsMap.entrySet()) {
            List<UserCoupon> userCoupons = entry.getValue();
            String message = userCoupons.size() > 1 ?
                    String.format("您有%d枚红包就要过期啦，赶紧 >>点我<< 去下单吧~", userCoupons.size()) :
                    String.format("您有1枚%f元的红包就要过期啦，赶紧 >>点我<< 去下单吧~", userCoupons.get(0).getDiscount());
            imServiceApi.push(entry.getKey(), message, "duola://home");
        }
    }
}
