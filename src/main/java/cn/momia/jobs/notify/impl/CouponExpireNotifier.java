package cn.momia.jobs.notify.impl;

import cn.momia.api.course.CouponServiceApi;
import cn.momia.jobs.notify.Notifier;
import org.springframework.beans.factory.annotation.Autowired;

public class CouponExpireNotifier implements Notifier {
    @Autowired private CouponServiceApi couponServiceApi;

    @Override
    public void notifyUser() {

    }
}
