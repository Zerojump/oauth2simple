package com.cmy.hoteltmallgenie.cache;

import com.alibaba.fastjson.JSON;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * <p>@author chenmingyi
 * <p>@version 1.0
 * <p>date: 2019/8/4
 */
@Component
public class HotelCache {
    //<skillId,skill> 假如001为技能开窗window_on的技能id，则 <001,window_on=1>，
    private Map<String, String> skillMap;
    //<hotelId,[roomName1,roomName2,...]> 酒店 -> 房间集合的map
    private Map<String, Set<String>> hotelAndRoomsMap;

    @PostConstruct
    public void init() throws IOException {
        Resource resource = new ClassPathResource("hotel_data.json");
        HotelData hotelData = JSON.parseObject(resource.getInputStream(), HotelData.class);
        this.skillMap = new ConcurrentHashMap<>(hotelData.skillMap);
        this.hotelAndRoomsMap = new ConcurrentHashMap<>(hotelData.hotelAndRoomsMap);
    }

    public String getSkill(String skillId) {
        return skillMap.get(skillId);
    }

    public boolean hotelAndRoomValid(String hotelId, String roomName) {
        Set<String> rooms = hotelAndRoomsMap.get(hotelId);
        return rooms != null && rooms.contains(roomName);
    }

    public Set<Map.Entry<String, String>> getAllSkills() {
        return skillMap.entrySet();
    }

    void setSkillMap(ConcurrentHashMap<String, String> skillMap) {
        this.skillMap = skillMap;
    }

    void setHotelAndRoomsMap(ConcurrentHashMap<String, Set<String>> hotelAndRoomsMap) {
        this.hotelAndRoomsMap = hotelAndRoomsMap;
    }

    private static class HotelData {
        private Map<String, String> skillMap;
        private Map<String, Set<String>> hotelAndRoomsMap;

        public Map<String, String> getSkillMap() {
            return skillMap;
        }

        public void setSkillMap(Map<String, String> skillMap) {
            this.skillMap = skillMap;
        }

        public Map<String, Set<String>> getHotelAndRoomsMap() {
            return hotelAndRoomsMap;
        }

        public void setHotelAndRoomsMap(Map<String, Set<String>> hotelAndRoomsMap) {
            this.hotelAndRoomsMap = hotelAndRoomsMap;
        }
    }

}







