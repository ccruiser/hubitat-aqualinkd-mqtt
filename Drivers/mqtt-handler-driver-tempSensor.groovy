// Copyright (C) Casey Cruise 2023

// See LICENSE.txt for licensing terms of this software.

// Child device type used by MQTT Connection for tempSensor to message events (no action)

// Installation:

// Copy this file and mqtt-conn-driver-aqualink.groovy as "New Drivers" in the user
// driver area of the Hubitat UI.

// Usage:

// This device type is not designed to be used on its own. Devices of this type
// are created as "component" devices of a MQTT Connection device using the
// latter's Add Handler custom command. Once created, you can refer to the
// "message" custom attribute of MQTT Handler devices in RM triggers and
// actions.

/*
 * Known Issue(s) & Gaps: 
 * 
 * Version Control:
 *
 * 0.1.0 - Initial version based on mq-handler-driver-listener
 * 
 * Thank you(s):
 * Kirk Rader for orginal code base and foundation of MQTT Setup
 */

// Returns the driver name
def DriverName(){
    return "MQTT Handler TempSensor"
}

//Handler Static items
def HandlerType() {
    return "TempSensor"
}

metadata {

  definition (name: "MQTT Handler TempSensor",
              namespace: "ccruiser",
              author: "Casey Cruise",
              importUrl: "https://raw.githubusercontent.com/ccruiser/hubitat-aqualinkd-mqtt/main/Drivers/mqtt-handler-driver-tempSensor.groovy") {

    // Declare a capability that is compatible with RM
    capability "Initialize"
    // Temp Sensor Items
    capability "TemperatureMeasurement"
    capability "Refresh"

    // Payload of incoming message.
    attribute "temperature", "STRING"
    attribute "payload", "STRING"
    attribute "topic", "STRING"
    attribute "Last Published", "STRING"
    attribute "Handler Type", "STRING"


  }
  preferences {
     if( ShowAllPreferences || ShowAllPreferences == null ){ // Show the preferences options
        section("Device Options:")  {
            input(
                name: "tempOffset",
                type: "number",
                title: "Temperature Offset Value",
                description: "Adjusted Temperature to Set",
                defaultValue: 0
            )
        }
        section("Logging Options:")  {
            input(
                    name: "debugLogEnable",
                    type: "bool",
                    title: "Enable debug logging",
                    defaultValue: false
            )
            input(
                    name: "infoLogEnable",
                    type: "bool",
                    title: "Enable info logging",
                    defaultValue: false
            )
            input(
                    name: "autoDisableDebugLog",
                    type: "number",
                    title: "Auto-disable debug logging",
                    description: "Automatically disable debug logging after this number of minutes (0 = Do not disable)",
                    defaultValue: 15
            )
            input(
                    name: "autoDisableInfoLog",
                    type: "number",
                    title: "Disable Info Logging",
                    description: "Automatically disable info logging after this number of minutes (0 = Do not disable)",
                    defaultValue: 15
            )
        }
        section("Show All Preferences:") {
                input( type: "bool", 
                name: "ShowAllPreferences", 
                title: "<b>Show All Preferences?</b>", 
                defaultValue: true )
        }
    }
    else {
        section("Show All Preferences:") {
            input( type: "bool", 
            name: "ShowAllPreferences", 
            title: "<b>Show All Preferences?</b>", 
            defaultValue: true )
        }
    }
  }
}

// [Driver API] Called when Device is first created
def installed() {

  //setup logging disable schedules
  unschedule()
  if (settings.debugLogEnable && settings.autoDisableDebugLog > 0)
      runIn(settings.autoDisableDebugLog * 60, disableDebugLog)
  if (settings.infoLogEnable && settings.autoDisableInfoLog > 0)
      runIn(settings.autoDisableInfoLog * 60, disableInfoLog)
  initialize()

}

// [Driver API] Called when Device's preferences are changed
void updated() {
  infoLog("Preferences updated/changed...")
  initialize()

}

// [Driver API] Called when Device receives a message
void parse(String description) { }

// [Capability Initialize]
void initialize() {

 // Default Handler Type to Listener
 sendEvent(name: "Handler Type", value: "${ HandlerType() }", isStateChange: true)

}

// [Capability Refresh]
void refresh() {
    parent.refresh()
}

// Calculate temperature with 0.01 precision in C or F unit as set by hub location settings
private parseTemperature(Float pTemp, checkin) {
  /*
	//float temp = hexStrToSignedInt(hexString)/100
	def tempScale = location.temperatureScale
	def debugText = "Reported temperature: raw = $temp°C"
	if (temp < -50) {
		log.warn "${device.displayName}: Out-of-bounds temperature value received. Battery voltage may be too low."
	} else {
		if (tempScale == "F") {
			temp = ((temp * 1.8) + 32)
			debugText += ", converted = $temp°F"
		}
		if (tempOffset) {
			temp = (temp + tempOffset)
			debugText += ", offset = $tempOffset"
		}
		displayDebugLog(debugText)
		temp = temp.round(2)
		generateEvent([
			name: 'temperature',
			value: temp,
			unit: "°$tempScale",
			descriptionText: "Temperature is $temp°$tempScale",
			translatable:true
			], checkin)
	}*/

}

void updateState(Float pTemp, String label) {
    def configuredUnit = parent.device.currentValue("aqualinkd_temp_scale") as String
    // round Temperature to two decimal points
    float rTemp = pTemp.round(2) 
    if (rTemp < -20) 
    {
      warnLog("${device.displayName}: Out-of-bounds temperature value received; check device settings")
    }
    def tempOut = Float.toString(rTemp)

    infoLog("update state function called for name: temperature value: "+
            tempOut+" from value: "+device.currentValue("temperature"+
            "with units: "+configuredUnit))
    //sendEvent(name: "temperature", value: tempOut, unit: configuredUnit)
    	generateEvent([
			name: 'temperature',
			value: tempOut,
			unit: "°$configuredUnit",
			descriptionText: "Temperature is $tempOut°$configuredUnit",
			translatable:true
			], true)
	}
}

/*---------------------------
 * Logging Helper Functions
 *---------------------------
 */
private void debugLog(String msg) {
    if (settings.debugLogEnable)
        log.debug("${device.label?device.label:device.name}: ${msg}")
}

private void infoLog(String msg) {
    if (settings.infoLogEnable)
        log.info("${device.label?device.label:device.name}: ${msg}")
}

private void warnLog(String msg) {
    log.warn("${device.label?device.label:device.name}: ${msg}")
}

private void errorLog(String msg) {
    log.error("${device.label?device.label:device.name}: ${msg}")
}

private void disableDebugLog() {
    infoLog("Automatically disabling debug logging after ${settings.autoDisableDebugLog} minutes.")
    device.updateSetting("debugLogEnable", [value: "false", type: "bool"])
}

private void disableInfoLog() {
    infoLog("Automatically disabling info logging after ${settings.autoDisableInfoLog} minutes.")
    device.updateSetting("infoLogEnable", [value: "false", type: "bool"])
}
