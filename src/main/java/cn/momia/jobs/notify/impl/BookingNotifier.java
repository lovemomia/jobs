package cn.momia.jobs.notify.impl;

import cn.momia.api.course.CourseServiceApi;
import cn.momia.api.course.dto.course.Course;
import cn.momia.api.course.dto.course.CourseSku;
import cn.momia.api.course.dto.course.CourseSkuPlace;
import cn.momia.api.im.ImServiceApi;
import cn.momia.api.user.UserServiceApi;
import cn.momia.common.core.util.TimeUtil;
import cn.momia.jobs.notify.Notifier;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class BookingNotifier implements Notifier {
    @Autowired private CourseServiceApi courseServiceApi;
    @Autowired private ImServiceApi imServiceApi;
    @Autowired private UserServiceApi userServiceApi;

    @Override
    public void notifyUser() {
        List<CourseSku> skus = courseServiceApi.queryCourseSkusClosedToday();
        if (skus.isEmpty()) return;

        Collections.sort(skus, new Comparator<CourseSku>() {
            @Override
            public int compare(CourseSku sku1, CourseSku sku2) {
                if (sku1.getParentId() < sku2.getParentId()) return -1;
                else if (sku1.getParentId() == sku2.getParentId()) return 0;
                else return 1;
            }
        });

        Map<Long, CourseSku> skusMap = new HashMap<Long, CourseSku>();
        for (CourseSku sku : skus) {
            if (sku.getParentId() <= 0) {
                skusMap.put(sku.getId(), sku);
            } else {
                CourseSku parentSku = skusMap.get(sku.getParentId());
                if (parentSku == null) continue;

                parentSku.setBooked(parentSku.getBooked() + sku.getBooked());
            }
        }
        if (skusMap.isEmpty()) return;

        Set<Long> courseIds = new HashSet<Long>();
        List<CourseSku> validSkus = new ArrayList<CourseSku>();
        for (Map.Entry<Long, CourseSku> entry : skusMap.entrySet()) {
            CourseSku sku = entry.getValue();
            if (sku.getBooked() >= sku.getMinBooked()) {
                courseIds.add(sku.getCourseId());
                validSkus.add(sku);
            }
        }
        if (courseIds.isEmpty()) return;

        List<Course> courses = courseServiceApi.list(courseIds);
        Map<Long, Course> coursesMap = new HashMap<Long, Course>();
        for (Course course : courses) {
            coursesMap.put(course.getId(), course);
        }

        for (CourseSku sku: validSkus) {
            Course course = coursesMap.get(sku.getCourseId());
            if (course == null) continue;

            CourseSkuPlace place = sku.getPlace();
            if (place == null) continue;

            List<Long> userIds = courseServiceApi.queryBookedUserIds(sku.getId());
            if (userIds.isEmpty()) continue;

            String date = TimeUtil.SHORT_DATE_FORMAT.format(sku.getStartTime()) + "（" + TimeUtil.getWeekDay(sku.getStartTime()) + "）";
            String time = sku.getTime();
            String address = place.getAddress();

            imServiceApi.pushGroup(sku.getId(), String.format("明天%s就要开课了，果果友情提醒：上课时间：%s，地点：%s，请大家提前10分钟到场签到，其他事项可以查看右上角的群公告哦~", course.getKeyWord(), time, address), "");
            userServiceApi.notifyBatch(userIds, String.format("亲，%s的%s就要开课了，上课时间：%s，地点：%s。请提前10分钟到场签到哦~", date, course.getKeyWord(), time, address));
        }
    }
}
