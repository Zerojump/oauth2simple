package com.cmy.hoteltmallgenie.controller;

import com.cmy.hoteltmallgenie.cache.HotelCache;
import com.cmy.hoteltmallgenie.cache.TaskService;
import com.cmy.hoteltmallgenie.service.UdpCommandSender;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * <p>@author chenmingyi
 * <p>@version 1.0
 * <p>date: 2019/8/4
 */
@RestController
@RequestMapping("hotel")
public class CommandController {
    @Value("${udp.server.protocolPrefix}")
    private String protocolPrefix;

    @Autowired
    private UdpCommandSender sender;

    @Autowired
    private HotelCache hotelCache;

    @Autowired
    private TaskService taskService;

    /**
     * 接收天猫指令
     */
    @RequestMapping(value = "control")
    public RespWrap control(HttpServletRequest req, TmallGenieCtrlMsg msg) {
        String skill = hotelCache.getSkill(msg.skillId);
        if (StringUtils.isEmpty(skill)) {
            return fail("unsupported skillId");
        } else {
            String controlReq = buildCtrlReq(protocolPrefix, msg.hotelId, msg.roomName, skill);
            sender.send(controlReq);

            if (!hotelCache.hotelAndRoomValid(msg.hotelId, msg.roomName)) {
                return ok(String.format("The command has been sent, but maybe the hotel['%s'] or the room['%s'] isn't exist", msg.hotelId, msg.roomName));
            }
            return ok();
        }
    }

    /**
     * 更新酒店缓存
     */
    @RequestMapping(value = "updateCache")
    public void updateCache( ) {
        taskService.updateDeviceSkillAndRooms();
    }

    public static String buildCtrlReq(String prifex, String hotelId, String roomName, String skill) {
        return String.format("%s/hotel_id=%s/room_name=%s/%s", prifex, hotelId, roomName, skill);
    }

    private static RespWrap fail(String error) {
        RespWrap wrap = new RespWrap();
        wrap.is_error = 1;
        wrap.error = error;
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        attributes.getResponse().setStatus(HttpServletResponse.SC_BAD_REQUEST);
        return wrap;
    }

    private static RespWrap ok() {
        return new RespWrap();
    }

    private static RespWrap ok(String msg) {
        RespWrap wrap = new RespWrap();
        wrap.error = msg;
        return wrap;
    }

    private static class RespWrap {
        private int is_error;
        private String error;

        public int getIs_error() {
            return is_error;
        }

        public void setIs_error(int is_error) {
            this.is_error = is_error;
        }

        public String getError() {
            return error;
        }

        public void setError(String error) {
            this.error = error;
        }

        @Override
        public String toString() {
            return "RespWrap{" +
                    "is_error=" + is_error +
                    ", error='" + error + '\'' +
                    '}';
        }
    }

    private static class TmallGenieCtrlMsg {
        String hotelId;
        String roomName;
        String skillId;

        public String getHotelId() {
            return hotelId;
        }

        public void setHotelId(String hotelId) {
            this.hotelId = hotelId;
        }

        public String getRoomName() {
            return roomName;
        }

        public void setRoomName(String roomName) {
            this.roomName = roomName;
        }

        public String getSkillId() {
            return skillId;
        }

        public void setSkillId(String skillId) {
            this.skillId = skillId;
        }

        @Override
        public String toString() {
            return "TmallGenieCtrlMsg{" +
                    "hotelId='" + hotelId + '\'' +
                    ", roomName='" + roomName + '\'' +
                    ", skillId='" + skillId + '\'' +
                    '}';
        }
    }

}
