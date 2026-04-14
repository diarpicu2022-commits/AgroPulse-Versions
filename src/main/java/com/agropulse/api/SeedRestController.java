package com.agropulse.api;

import com.agropulse.dao.*;
import com.agropulse.model.*;
import com.agropulse.model.enums.SensorType;
import com.agropulse.model.enums.ActuatorType;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;

public class SeedRestController extends JsonRestController {

    private static final SensorDao sensorDao = new SensorDao();
    private static final CropDao cropDao = new CropDao();
    private static final ActuatorDao actuatorDao = new ActuatorDao();
    private static final Gson gson = new Gson();

    @Override
    public void handle(String path, String method, HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            if ("GET".equals(method) || "POST".equals(method)) {
                seedData(resp);
            } else {
                sendError(resp, 405, "Method not allowed");
            }
        } catch (Exception e) {
            sendError(resp, 500, e.getMessage());
        }
    }

    private void seedData(HttpServletResponse resp) throws SQLException, IOException {
        // Seed sensors if none exist
        if (sensorDao.findAll().isEmpty()) {
            sensorDao.save(new Sensor("Temperatura Interna", SensorType.TEMPERATURE_INTERNAL, "Centro"));
            sensorDao.save(new Sensor("Temperatura Externa", SensorType.TEMPERATURE_EXTERNAL, "Techo"));
            sensorDao.save(new Sensor("Humedad Relativa", SensorType.HUMIDITY, "Lateral"));
            sensorDao.save(new Sensor("Humedad del Suelo", SensorType.SOIL_MOISTURE, "Suelo"));
        }

        // Seed crops if none exist
        if (cropDao.findAll().isEmpty()) {
            cropDao.save(new Crop("Tomate", "Cherry", 18.0, 28.0, 60.0, 80.0, 6.0, 7.0));
            cropDao.save(new Crop("Lechuga", "Iceberg", 15.0, 22.0, 70.0, 90.0, 6.0, 7.5));
            cropDao.save(new Crop("Pimentón", "California", 20.0, 30.0, 65.0, 85.0, 6.0, 7.0));
        }

        // Seed actuators if none exist
        if (actuatorDao.findAll().isEmpty()) {
            Actuator a1 = new Actuator("Extractor 1", ActuatorType.EXTRACTOR);
            a1.setEnabled(true); a1.setAutoMode(true);
            actuatorDao.save(a1);
            
            Actuator a2 = new Actuator("Bomba Riego", ActuatorType.WATER_PUMP);
            a2.setEnabled(true); a2.setAutoMode(false);
            actuatorDao.save(a2);
            
            Actuator a3 = new Actuator("Calefactor", ActuatorType.HEAT_GENERATOR);
            a3.setEnabled(true); a3.setAutoMode(false);
            actuatorDao.save(a3);
            
            Actuator a4 = new Actuator("Puerta Principal", ActuatorType.DOOR);
            a4.setEnabled(true); a4.setAutoMode(false);
            actuatorDao.save(a4);
        }

        JsonObject result = new JsonObject();
        result.addProperty("seeded", true);
        result.addProperty("sensors", 4);
        result.addProperty("crops", 3);
        result.addProperty("actuators", 4);
        sendJson(resp, result);
    }
}