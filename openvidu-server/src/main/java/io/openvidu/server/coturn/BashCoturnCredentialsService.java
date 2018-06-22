package io.openvidu.server.coturn;

import java.io.IOException;

import org.apache.commons.lang3.RandomStringUtils;

import io.openvidu.server.CommandExecutor;
import io.openvidu.server.config.OpenviduConfig;

public class BashCoturnCredentialsService extends CoturnCredentialsService {

	public BashCoturnCredentialsService(OpenviduConfig openviduConfig) {
		super(openviduConfig);
		try {
			String response = CommandExecutor.execCommand("/bin/sh", "-c",
					"turnadmin -l -N " + this.coturnDatabaseString);
			if (response.contains("turnadmin: not found")) {
				// No coturn installed in the host machine
				log.warn("No COTURN server is installed in the host machine. Response: " + response);
				log.warn("No COTURN server will be automatically configured for clients");
				this.coturnAvailable = false;
			} else if (response.contains("Cannot initialize Redis DB connection")) {
				log.warn("Redis DB is not accesible with connection string " + this.coturnDatabaseString);
				log.warn("No COTURN server will be automatically configured for clients");
				this.coturnAvailable = false;
			} else {
				log.info("COTURN Redis DB accessible with string " + this.coturnDatabaseString);
			}
		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
		}
		log.info("Using COTURN credentials service for BASH environment");
	}

	@Override
	public TurnCredentials createUser() {
		TurnCredentials credentials = null;
		log.info("Creating COTURN user");
		String user = RandomStringUtils.randomAlphanumeric(6).toUpperCase();
		String pass = RandomStringUtils.randomAlphanumeric(6).toLowerCase();
		String command = "turnadmin -a -u " + user + " -r openvidu -p " + pass + " -N " + this.coturnDatabaseString;
		try {
			String response = CommandExecutor.execCommand("/bin/sh", "-c", command);
			if (response.contains("connection success: " + this.trimmedCoturnDatabaseString)) {
				credentials = new TurnCredentials(user, pass);
				log.info("COTURN user created: true");
			} else {
				log.info("COTURN user created: false");
			}
		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
		}
		return credentials;
	}

	@Override
	public boolean deleteUser(String user) {
		boolean userRemoved = false;

		log.info("Deleting COTURN user");
		String command = "turnadmin -d -u " + user + " -r openvidu -N " + this.coturnDatabaseString;
		String response = "";
		try {
			response = CommandExecutor.execCommand("/bin/sh", "-c", command);
		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
		}
		userRemoved = response.contains("connection success: " + this.trimmedCoturnDatabaseString);
		log.info("COTURN user deleted: " + userRemoved);
		return userRemoved;
	}

}