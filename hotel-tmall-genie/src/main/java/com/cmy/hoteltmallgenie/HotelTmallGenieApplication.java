package com.cmy.hoteltmallgenie;

import com.cmy.hoteltmallgenie.service.UdpCommandSender;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class HotelTmallGenieApplication {

	@Value("${udp.server.host}")
	private String udpServerHost;
	@Value("${udp.server.port}")
	private int udpServerPort;

	@Bean(destroyMethod = "close")
	public UdpCommandSender sender() {
		return new UdpCommandSender(udpServerHost,udpServerPort);
	}

	public static void main(String[] args) {
		SpringApplication.run(HotelTmallGenieApplication.class, args);
	}

}
