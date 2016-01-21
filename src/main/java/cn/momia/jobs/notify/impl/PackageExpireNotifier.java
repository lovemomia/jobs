package cn.momia.jobs.notify.impl;

import cn.momia.api.course.OrderServiceApi;
import cn.momia.api.im.ImServiceApi;
import cn.momia.api.user.UserServiceApi;
import cn.momia.common.webapp.config.Configuration;
import cn.momia.jobs.notify.Notifier;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

public class PackageExpireNotifier implements Notifier {
    @Autowired OrderServiceApi orderServiceApi;
    @Autowired ImServiceApi imServiceApi;
    @Autowired UserServiceApi userServiceApi;

    @Override
    public void notifyUser() {
        notifyAhead(3);
        notifyAhead(10);
    }

    private void notifyAhead(int days) {
        List<Long> userIds = orderServiceApi.queryUserIdsOfPackagesToExpired(days);
        if (userIds.isEmpty()) return;

        imServiceApi.pushBatch(userIds, String.format("您有课程(包)还有%d天就要过期了，抓紧来选课吧~\n>> 点我 << 去选课", days), Configuration.getString("AppConf.Name") + "://bookedcourselist");
        userServiceApi.notifyBatch(userIds, String.format("您有课程(包)还有%d天就要过期了，抓紧来选课吧~ http://" + Configuration.getString("Wap.Domain") + "/user/bookable", days));
    }
}
