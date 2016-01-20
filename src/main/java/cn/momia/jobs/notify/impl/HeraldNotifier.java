package cn.momia.jobs.notify.impl;

import cn.momia.api.course.CourseServiceApi;
import cn.momia.api.course.OrderServiceApi;
import cn.momia.api.im.ImServiceApi;
import cn.momia.common.webapp.config.Configuration;
import cn.momia.jobs.notify.Notifier;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

public class HeraldNotifier implements Notifier {
    @Autowired private CourseServiceApi courseServiceApi;
    @Autowired private OrderServiceApi orderServiceApi;
    @Autowired private ImServiceApi imServiceApi;

    @Override
    public void notifyUser() {
        List<String> courses = courseServiceApi.queryHotNewCourses();
        if (courses.isEmpty()) return;

        List<Long> userIds = orderServiceApi.queryBookableUserIds();
        imServiceApi.pushBatch(userIds, "本周精彩课程有：" + StringUtils.join(courses, ",") + "，还没有选课的亲快来 >> 点我 << 去看看吧~", Configuration.getString("AppConf.Name") + "://home");
    }
}
