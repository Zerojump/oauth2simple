package com.cmy.hoteltmallgenie.cache;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * <p>@author chenmingyi
 * <p>@version 1.0
 * <p>date: 2019/8/4
 */
@Component
public class TaskService {
    @Autowired
    private HotelCache hotelCache;

    //每半个小时更新一次技能
    @Scheduled(fixedDelay = 1800000)
    public void updateDeviceSkillAndRooms() {

    }
}
