package cn.momia.jobs.notify.impl;

import cn.momia.api.course.CourseServiceApi;
import cn.momia.api.course.OrderServiceApi;
import cn.momia.api.im.ImServiceApi;
import cn.momia.common.webapp.config.Configuration;
import cn.momia.jobs.notify.Notifier;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class HeraldNotifier implements Notifier {
    @Autowired private CourseServiceApi courseServiceApi;
    @Autowired private OrderServiceApi orderServiceApi;
    @Autowired private ImServiceApi imServiceApi;

    @Override
    public void notifyUser() {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date());
        if (calendar.get(Calendar.DAY_OF_WEEK) != Calendar.SUNDAY) return;

        List<String> courses = courseServiceApi.queryHotNewCourses();
        if (courses.isEmpty()) return;

        List<Long> userIds = orderServiceApi.queryBookableUserIds();
        imServiceApi.pushBatch(userIds, "本周精彩课程有：" + StringUtils.join(courses, ",") + "，还没有选课的亲快来\n>> 点我 << 去看看吧~", "http://" + Configuration.getString("Wap.Domain") + "/subject/list");
    }
}
