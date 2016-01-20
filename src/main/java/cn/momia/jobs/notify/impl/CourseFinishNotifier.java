package cn.momia.jobs.notify.impl;

import cn.momia.api.course.CourseServiceApi;
import cn.momia.api.im.ImServiceApi;
import cn.momia.common.webapp.config.Configuration;
import cn.momia.jobs.notify.Notifier;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

public class CourseFinishNotifier implements Notifier {
    @Autowired private CourseServiceApi courseServiceApi;
    @Autowired private ImServiceApi imServiceApi;

    @Override
    public void notifyUser() {
        List<Long> userIds = courseServiceApi.queryUserIdsOfTodaysCourse();
        imServiceApi.pushBatch(userIds, "美好的一天结束啦，小朋友在课堂上都表现得棒棒哒，粑粑和麻麻别忘记给我们的课程写评价哦~ >> 点我 << 去写评价", Configuration.getString("AppConf.Name") + "://bookedcourselist");
    }
}
