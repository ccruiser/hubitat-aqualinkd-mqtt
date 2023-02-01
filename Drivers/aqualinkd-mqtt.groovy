/*
* Aqualinkd-mqtt
*
* Description:
* This Hubitat enables aqualinkd integrations
*
* Overall Setup:
* 1) Add the Aqualinkd-Driver.groovy 
* 2) Set the varible options for the endpoint and devices
* 3) Configure MQTT (WIP)
*
* Licensing:
* Copyright 2023 Casey Cruise
* Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
* in compliance with the License. You may obtain a copy of the License at: http://www.apache.org/licenses/LICENSE-2.0
* Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
* on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
* for the specific language governing permissions and limitations under the License.
*
* Known Issue(s) & Gaps:
* 
* Version Control:
* 0.1.0 - Initial version based on logic from conn-driver and iAqualink
*       - added TempSensor and Air Temp logic
*   
* Thank you(s):
* Vyrolan for the iAqualink setup and Kirk Rader for the MQTT setup info
*
*/

// Returns the driver name
String DriverName(){
    return "AqualinkD-MQTT"
}

// Returns the driver version
String DriverVersion(){
    return "0.1.0"
}

// Returns the topic value for the air temperature
String TopicAirTemp(){
    return "aqualinkd/Temperature/Air"
}



metadata {
    definition(
            name: "AqualinkD-MQTT",
            namespace: "ccruiser",
            author: "Casey Cruise",
            importUrl: "https://raw.githubusercontent.com/ccruiser/hubitat-aqualinkd-mqtt/main/Drivers/aqualinkd-mqtt.groovy"
    ) {
        capability "Initialize"
        capability "Refresh"
        capability "Configuration"
        // Include a capability that is integrated with Rule Machine
        capability "Actuator"

        // Define the Current Driver attributes 
        attribute "Driver Name", "string" // Identifies the driver being used for update purposes
		    attribute "Driver Version", "string" // Handles version for driver

        // Define connection tracking staus vars to MQTT broker ("connected" or "disconnected").
        attribute "MQTT Connection Status", "STRING"
        attribute "MQTT Connection Last Event", "STRING"

        // Define Generic Driver Vars
        attribute "online", "enum", [ "off", "on" ]
        attribute "aqualinkd_temp_scale", "string"
        attribute "config_temp_scale", "string"
        //Define Pool Temperature Device Vars
        attribute "air_temp", "number"
        attribute "pool_temp", "number"
        attribute "spa_temp", "number"
        attribute "pool_set_point", "number"
        attribute "spa_set_point", "number"
        // Define Main Pool Pump & Heater Vars
        attribute "freeze_protection", "enum", [ "off", "on" ]
        attribute "spa_mode", "enum", [ "off", "on" ]
        attribute "filter_pump", "enum", [ "off", "on" ]
        attribute "spa_heater", "enum", [ "off", "enabled", "heating" ]
        attribute "pool_heater", "enum", [ "off", "enabled", "heating" ]
        attribute "solar_heater", "enum", [ "off", "on" ]
        // Define Aux Vars
        attribute "aux1", "enum", [ "off", "on" ]
        attribute "aux1_label", "string"
        attribute "aux2", "enum", [ "off", "on" ]
        attribute "aux2_label", "string"
        attribute "aux3", "enum", [ "off", "on" ]
        attribute "aux3_label", "string"
        attribute "aux4", "enum", [ "off", "on" ]
        attribute "aux4_label", "string"
        attribute "aux5", "enum", [ "off", "on" ]
        attribute "aux5_label", "string"
        attribute "aux6", "enum", [ "off", "on" ]
        attribute "aux6_label", "string"
        attribute "aux7", "enum", [ "off", "on" ]
        attribute "aux7_label", "string"
        attribute "aux8", "enum", [ "off", "on" ]
        attribute "aux8_label", "string"
        // Define One Touch Vars
        attribute "onetouch1", "enum", [ "off", "on" ]
        attribute "onetouch1_label", "string"
        attribute "onetouch2", "enum", [ "off", "on" ]
        attribute "onetouch2_label", "string"
        attribute "onetouch3", "enum", [ "off", "on" ]
        attribute "onetouch3_label", "string"
        attribute "onetouch4", "enum", [ "off", "on" ]
        attribute "onetouch4_label", "string"
        attribute "onetouch5", "enum", [ "off", "on" ]
        attribute "onetouch5_label", "string"
        attribute "onetouch6", "enum", [ "off", "on" ]
        attribute "onetouch6_label", "string"
        // Define Other Pool Vars
        attribute "cover_pool", "string"
        attribute "spa_salinity", "string"
        attribute "pool_salinity", "string"
        attribute "orp", "string"
        attribute "ph", "string"
    }

    preferences {
        if( ShowAllPreferences || ShowAllPreferences == null ){ // Show the preferences options
            /*section("Device Settings:") {
                
                input(
                        name: "apiEmail",
                        type: "string",
                        title: "API Email",
                        description: "iAqualink Login Email",
                        required: true,
                        displayDuringSetup: true
                )
                input(
                        name: "apiPassword",
                        type: "string",
                        title: "Password",
                        description: "iAqualink Login Password",
                        required: true,
                        displayDuringSetup: true
                )
                
                input(
                        name: "deviceIndex",
                        type: "number",
                        title: "Device Index",
                        description: "Index within list of devices on the iAqualink account. Zero-based. Default 0 to select first (or only) device should work for most users.",
                        defaultValue: 0,
                        displayDuringSetup: true
                )
                
            }
            */
            section("MQTT Settings:") {
            input(
                name: "mqttBroker",
                type: "text",
                title: "Broker IP/SERVER",
                description: "input the broker endpoint to connect",
                required: true,
                displayDuringSetup: true
            )
             input(
                name: "isMqttEncrypted",
                type: "bool",
                title: "Use SSL/TLS for Broker traffic",
                description: "if set to true then ss:// will be pre-fixed; otherwise tcp://",
                required: true,
                defaultValue: false,
                displayDuringSetup: true
                )

            input(
                name: "mqttClientId",
                type: "text",
                title: "Client Id",
                description: "MQTT client id",
                required: true,
                displayDuringSetup: true,
                defaultValue: UUID.randomUUID().toString()
            )

            input(
                name: "mqttUserName",
                type: "string",
                title: "MQTT Username",
                description: "(optional user-name)",
                required: false,
                displayDuringSetup: true
            )

            input(
                name: "mqttPassword",
                type: "password",
                title: "MQTT Password",
                description: "(optional password)",
                required: false,
                displayDuringSetup: true
            )

            input(
                name: "lwtTopic",
                type: "text",
                title: "LWT Topic",
                description: "Last-Will-Testiment message topic when disconnecting from broker",
                required: true,
                displayDuringSetup: true,
                defaultValue: "hubitat/lwt"
            )

            input(
                name: "lwtMessage",
                type: "text",
                title: "LWT Message",
                description: "Last-Will-Testiment message body when disconnecting from broker",
                required: true,
                displayDuringSetup: true,
                defaultValue: "disconnected"
            )
            }
            section("Pool/Spa Settings:") {
                input(
                        name: "spaMode",
                        type: "bool",
                        title: "Enable Control for Spa Mode",
                        defaultValue: false,
                        displayDuringSetup: true
                )
                input(
                        name: "poolHeater",
                        type: "bool",
                        title: "Enable Pool Heater",
                        defaultValue: false,
                        displayDuringSetup: true
                )
                input(
                        name: "spaHeater",
                        type: "bool",
                        title: "Enable Spa Heater",
                        defaultValue: false,
                        displayDuringSetup: true
                )
                input(
                        name: "solarHeater",
                        type: "bool",
                        title: "Enable Solar Heater",
                        defaultValue: false,
                        displayDuringSetup: true
                )
                input (
                        name: "tempUnit",
                        type: "enum",
                        title: "Temperature Unit",
                        description: "Unit used for Temperature values. Defaults to the hub's configured unit but can be forced to F or C here.",
                        options: [
                                "hub": "Hub's Configured Unit (default)",
                                "C": "Celsius",
                                "F": "Fahrenheit"
                        ],
                        required: true,
                        defaultValue: "hub",
                        displayDuringSetup: true
                )
                input (
                        name: "airTemp",
                        type: "bool",
                        title: "Enable Air Temp Sensor",
                        description: "Include separate temperature sensor child device with the air temperature? (Parent device always has the attribute.)",
                        defaultValue: false,
                        displayDuringSetup: true
                )
                input (
                        name: "poolTemp",
                        type: "bool",
                        title: "Enable Pool Temp Sensor",
                        description: "Include separate temperature sensor child device with the pool temperature? (Parent device always has the attribute.)",
                        defaultValue: false,
                        displayDuringSetup: true
                )
                input (
                        name: "spaTemp",
                        type: "bool",
                        title: "Enable Spa Temp Sensor",
                        description: "Include separate temperature sensor child device with the spa temperature? (Parent device always has the attribute.)",
                        defaultValue: false,
                        displayDuringSetup: true
                )
            }
            section("Auxillary:") {
                input(
                        name: "numAux",
                        type: "number",
                        title: "Number of AUX Devices",
                        description: "How many of the AUX devices to include? (Default 7 is max.)",
                        defaultValue: 7,
                        displayDuringSetup: true
                )
                input(
                        name: "auxEA",
                        type: "bool",
                        title: "Enable AUX EA Port",
                        description: "Include the special 8th AUX device (which may show simply as 'N/A 8' in the iAqualink mobile app)?",
                        defaultValue: false,
                        displayDuringSetup: true
                )
                /*
                input(
                        name: "numOneTouch",
                        type: "number",
                        title: "Number of OneTouch Setups",
                        description: "How many of the OneTouch setups to include? (Default 6 is max.)",
                        defaultValue: 6,
                        displayDuringSetup: true
                )
                */
            }
            section("Logging:") {
                /*
                input(
                        name: "autoUpdateInterval",
                        type: "number",
                        title: "Auto Update Interval Minutes (0 = Disabled)",
                        description: "Number of minutes between automatic updates to pull latest status",
                        defaultValue: 10,
                        displayDuringSetup: true
                )
                */
                input(
                        name: "debugLogEnable",
                        type: "bool",
                        title: "Enable debug logging",
                        defaultValue: true
                )
                input(
                        name: "infoLogEnable",
                        type: "bool",
                        title: "Enable info logging",
                        defaultValue: true
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
        } else {
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
void installed() {
    
    state.auth_time = 0
    //initialize()
    state.handlers = [:]

    synchronized (state.handlers) {

        infoLog("initializing mqtt connection for installed device")
        sendEvent(name: "MQTT Connection Status", value: "disconnected")
        sendEvent(name: "MQTT Connection Last Event", value: new Date() )
        // Check for null / empty broker
        if (settings.mqttBroker?.trim()) {
            initialize()
        }
  }
}

// [Driver API] Called when Device's preferences are changed
void updated() {
    infoLog("Preferences changed...")
    infoDebug("using broker connection string: ${BrokerURL()}")
    state.auth_time = 0 // force relogin after any settings changes
    initialize()
}

// [Driver API] Called when Device receives a message
//void parse(String description) { }

void debugLog(String msg) {
    if (settings.debugLogEnable)
        log.debug("${device.label?device.label:device.name}: ${msg}")
}

void infoLog(String msg) {
    if (settings.infoLogEnable)
        log.info("${device.label?device.label:device.name}: ${msg}")
}

void warnLog(String msg) {
    log.warn("${device.label?device.label:device.name}: ${msg}")
}

void errorLog(String msg) {
    log.error("${device.label?device.label:device.name}: ${msg}")
}

void disableDebugLog() {
    infoLog("Automatically disabling debug logging after ${settings.autoDisableDebugLog} minutes.")
    device.updateSetting("debugLogEnable", [value: "false", type: "bool"])
}

void disableInfoLog() {
    infoLog("Automatically disabling info logging after ${settings.autoDisableInfoLog} minutes.")
    device.updateSetting("infoLogEnable", [value: "false", type: "bool"])
}

void updateAttribute(args) {
    sendEvent(args)
}

// [Capability Initialize]
void initialize() {

    //Set Version and Driver
    sendEvent(name: "Driver Name", value: "${ DriverName() }")
    sendEvent(name: "Driver Version", value: "${ DriverVersion() }")
    //reset settings and other items
    unschedule()
    if (settings.debugLogEnable && settings.autoDisableDebugLog > 0)
        runIn(settings.autoDisableDebugLog * 60, disableDebugLog)
    if (settings.infoLogEnable && settings.autoDisableInfoLog > 0)
        runIn(settings.autoDisableInfoLog * 60, disableInfoLog)

    //Initialize Brokers
    synchronized (state.handlers) {
    debugLog("Initialization reset of broker connection to "+BrokerURL())
    disconnect()
    connect()

  }
  // check for a good mqtt connection state
  if (device.currentValue("MQTT Connection Status") == "connected")
  {
    ensureChildren()
  }
  else
  {
    warnLog("mq connection is not in a good status; no additional initialiation actions taken")
  }

    /*
    if (!ensureLogin())
        return

    ensureChildren()
    autoUpdate()
    */
    infoLog("Initialization completed for driver ")

    
}

////////////////////////////////////////////////////////////////////////////////
// Custom broker commands

// Connect to the MQTT broker.
void connect() {

  synchronized (state.handlers) {

    // keep trying to connect until successful
    // or some other thread indicates that this
    // attempt should cease

    state.reconnect = true
    state.handlers.notifyAll()

    while (!state.connected && state.reconnect) {

      try {

        interfaces.mqtt.connect(BrokerURL(), settings.mqttClientId,
                                settings.mqttUserName, settings.mqttPassword,
                                lastWillTopic: settings.lwtTopic, lastWillQos: 0,
                                lastWillMessage: settings.lwtMessage)

        state.connected = true
        state.handlers.notifyAll()
        infoLog( "connected to " + BrokerURL() )

        state.handlers.each { topic, subscription ->

          subscribe(topic, subscription.qos)

        }

        sendEvent(name: "MQTT Connection Status", value: "connected")
        sendEvent(name: "MQTT Connection Last Event", value: new Date() )


      } catch (e) {

        errorLog ("error connecting to MQTT broker: ${e}")
        sendEvent(name: "MQTT Connection Status", value: "error")
        sendEvent(name: "MQTT Connection Last Event", value: new Date() )
        state.handlers.wait(5000)

      }
    }
  }
}

// Disconnect from the MQTT broker.
void disconnect() {

  synchronized (state.handlers) {

    // kill any threads that are attempting
    // to connect
    state.reconnect = false
    state.handlers.notifyAll()

    if (state.connected) {

      state.handlers.each { topic, subscription ->

        unsubscribe(topic)

      }

      try {
        debugLog("MQTT attempting disconnect from "+BrokerURL())
        interfaces.mqtt.disconnect()


      } catch (e) {

        errorLoglog("error disconnecting MQTT broker: ${e}")

      }

      state.connected = false
      state.handlers.notifyAll()
      infoLog( "disconnected from " + BrokerURL() )
      sendEvent(name: "MQTT Connection Status", value: "disconnected")
      sendEvent(name: "MQTT Connection Last Event", value: new Date() )


    }
  }
}

// Publish the given payload on the given MQTT topic.
def publish(String topic, String payload, int qos = 2,
            boolean retained = false ) {

  synchronized (state.handlers) {

    try {

      interfaces.mqtt.publish(topic, payload, qos, retained)
      infoLog "published topic: "+topic+" with value: "+payload+" qos: "+qos+" retained: "+retained

    } catch (e) {

      errorLog ("error publishing ${payload} on topic ${topic}: ${e}")

    }
  }
}

// RM-compatible overload for publish(String, String, int, boolean).
def publish(String topic, String payload, int qos, String retained) {
  infoLog("topic: "+topic+" published called with payload: "+payload)
  publish(topic, payload, qos, retained.toBoolean())

}

// Add a MQTT Handler child device subscribed to the specified topic.
def addHandler(String name, String topic, int qos, String hType) {

  synchronized (state.handlers) {
    def id = UUID.randomUUID().toString()
    infoLog(" adding child device of type"+hType+" with name: "+name)
     switch( hType ){
            case "Switch":
                addChildDevice("ccruiser", "MQTT Handler Switch", id,
                  [isComponent: true, name: name])

            break
            case "TempSensor":
                addChildDevice("ccruiser", "MQTT Handler TempSensor", id,
                  [isComponent: true, name: name])

            break
            default:
                addChildDevice("ccruiser", "MQTT Handler Listener", id,
                [isComponent: true, name: name])
            break
     }
      subscribeHandler(topic, qos, id)
      return id

  }
}

// Delete the specified MQTT Handler child device and unsubscribe from the
// corresponding topic.
def deleteHandler(String id) {

  synchronized (state.handlers) {

    unsubscribeHandler(id)
    deleteChildDevice(id)

  }
}

// Replace the topic associated with the child device with given id.
def replaceTopic(String topic, int qos, String id) {

  synchronized (state.handlers) {

    unsubscribeHandler(id)
    subscribeHandler(topic, qos, id)

  }
}

////////////////////////////////////////////////////////////////////////////////
// MQTT client call-back functions

// Callback invoked by the MQTT interface on status changes and errors.
void mqttClientStatus(String message) {

  if (message.startsWith("Error:")) {

    errorLog( "mqttClientStatus: ${message}") 
    disconnect()
    runIn (5,"connect")

  } else {

    infoLog ("mqttClientStatus: ${message}" )

  }
}

// Callback invoked by the MQTT interface on receipt of a MQTT message.
//
// Looks up the corresponding MQTT Handler child device in state.handlers
// and forwards the message payload as an event.
//
// [Driver API] Called when Device receives a message
def parse(String event) {

  synchronized (state.handlers) {

    def message = interfaces.mqtt.parseMessage(event)

    for (element in state.handlers) {

      if (element.key.equals(message.topic)) {

        def handler = getChildDevice(element.value.id)

        if (handler == null) {

          warnLog( "parse: no child found with id ${element.value.id}; no action taken for topic "+message.topic )

        } else {

          debugLog("sending update from topic: "+message.topic+" for payload: "+message.payload)
          handler.sendEvent(name: "payload", value: message.payload)
          handler.sendEvent(name: "Last Published", value: new Date())
          
          // Check for Handler Types
          def defType = handler.currentValue("Handler Type")
          debugLog(" validating handler payload for handler action for type: "+defType)
            switch( defType ){
                  case "Switch":
                      debugLog(" setting 'switch' value per payload: "+message.payload)
                      switch (message.payload){
                        case "1":
                           handler.sendEvent(name: "switch", value: "on")
                        break
                        case "0":
                           handler.sendEvent(name: "switch", value: "off")
                        break
                        default:
                          warnLog("Unknown handler type: "+defType+" with payload "+message.payload )
                        break

                      }
                  break
                  case "TempSensor":
                      debugLog(" setting 'tempSensor' value per payload: "+message.payload)
                      handler.updateState(convertTemperature(message.payload),"")
                      //handler.sendEvent(name: "temperature", value: convertTemperature(message.payload),"F")

                  break
                  case "Listener":
                      debugLog(" setting 'listener' value per payload: "+message.payload)
                  break
                  default:
                    warnLog("Unknown handler type: "+defType )
                  break
          }

        }
      }
    }
  }
}

////////////////////////////////////////////////////////////////////////////////
// Helper functions

// Returns the driver name
String BrokerURL(){
    if(settings.isMqttEncrypted)
    {
        return "ssl://"+settings.mqttBroker
    }
    else{
        return "tcp://"+settings.mqttBroker 
    }
}


// Subscribe to the specified topic at the specified quality of service setting.
def subscribe(String topic, int qos = 2) {

  synchronized (state.handlers) {

    try {

      interfaces.mqtt.subscribe(topic, qos)
      infoLog ( "subscribed to ${topic}" )

    } catch (e) {

      errorLog ( "error subscribing to topic ${topic} (qos=${qos})" )

    }
  }
}

// Unsubscribe from the specified topic.
def unsubscribe(String topic) {

  synchronized (state.handlers) {

    try {

      interfaces.mqtt.unsubscribe(topic)
      infoLog ("unsubscribed from ${topic}")

    } catch (e) {

      errorLog ("error unsubscribing from topic ${topic}")

    }
  }
}

// Unsubscribe from the topic currently handled by the child device with the given id.
def unsubscribeHandler(String id) {

  synchronized (state.handlers) {

    // note: the version of groovy in hubitat seems to be missing Map.removeAll()

    def dTopic = null

    for (element in state.handlers) {

      if (element.value.id == id) {

        dTopic = element.key
        break

      }
    }
    //Unsubscribe from topic (if found)
    if (dTopic != null) {
      debugLog("unsubscribing events from topic: "+dTopic+"")
      state.handlers.remove(dTopic)
      unsubscribe(dTopic)

    }
    else{
        warnLog("unable to unsubscribing from topic: "+dTopic+" as no matching child was found; no action taken.")

    }
  }
}

// Subscribe to the given topic and associate it with the child device with the
// given id.
def subscribeHandler(String topic, int qos, String id) {

  synchronized (state.handlers) {

    def handler = getChildDevice(id)

    if (handler != null) {

      subscribe(topic, qos)
      state.handlers.putAt(topic, [id: id, qos: qos])
      debugLog("subscribing events for child device: "+handler+" for topic "+topic)
      handler.sendEvent(name: "topic", value: topic)
    } else {

      warnLog( "subscribeHandler: no child device found with id ${id}" )

    }
  }
}

////////////////////////////////////////////////////////////////////////////////
// Custom iAqualink HTTP Commands

// [Capability Refresh]
void refresh() {
    //Set Version and Driver
    sendEvent(name: "Driver Name", value: "${ DriverName() }")
    sendEvent(name: "Driver Version", value: "${ DriverVersion() }")
    updateStates()
}

// [Capability Configuration]
void configure() {
    //state.clear()
    installed()
}

def ensureLogin() {
    if (now() - state.auth_time < 50 * 60 * 1000) {
        // old session should still be good (less than 50m elapsed)
        debugLog("Skipping login and reusing previous session that should still be active.")
        return true
    }

    // TODO: do other stuff to indicate errors?
    if (!doLogin()) {
        return false
    }
    if (!getSerialNumber()) {
        return false
    }
    return true
}

def doLogin() {
    /*
    if (!(settings.apiEmail) || !(settings.apiPassword)) {
        errorLog("Both Email and Password must be set in preferences.")
        return false
    }
    */


    def url = "https://prod.zodiac-io.com/users/v1/login"

    def params = [
            uri: url,
            requestContentType: "application/json",
            body: """{"api_key": "EOOEMOW4YR6QNB07", "email": "${settings.apiEmail}", "password": "${settings.apiPassword}"}"""
    ]

    infoLog("Logging into iAqualink service...")

    def success = true
    httpPost(params) { response ->
        if (response?.status != 200) {
            errorLog("Login request failed (HTTP ${response?.status})")
            success = false
            return
        }

        state.session_id = response.data.session_id
        state.auth_token = response.data.authentication_token
        state.user_id = response.data.id
        state.auth_time = now()
        infoLog("Successfully logged in.")
    }
    return success
}

def doGetRequest(String desc, String url) {
    def params = [ uri: url ]

    def result = null
    httpGet(params) { response ->
        if (response?.status != 200) {
            errorLog("${desc} failed (HTTP ${response?.status})")
            return
        }
        result = response.data
    }
    return result
}

def doCommand(String desc, String command, LinkedHashMap<String, String> args = []) {
    def url = "https://p-api.iaqualink.net/v1/mobile/session.json?actionID=command&command=${command}&serial=${state.serial_number}&sessionID=${state.session_id}"
    if (args.size() > 0) {
        ArrayList params = []
        args.each{a -> params.add(a)}
        url += "&" + params.join("&")
    }
    debugLog("Executing iAqualink Command '${command}' with args = ${args}")
    return doGetRequest("Command ${command}", url)
}



def getHome() {
    if (!ensureLogin()) return []
    infoLog("Fetching home screen.")
    def response = doCommand("Fetching home screen", "get_home")
    if (!response) return []
    return response.home_screen
}

def getDevices() {
    if (!ensureLogin()) return []
    infoLog("Fetching devices.")
    def response = doCommand("Fetching devices", "get_devices")
    if (!response) return []
    return response.devices_screen
}

/*
def getOneTouch() {
    if (!ensureLogin()) return []
    infoLog("Fetching OneTouch.")
    def response = doCommand("Fetching OneTouch", "get_onetouch")
    if (!response) return []
    return response.onetouch_screen
}
*/

def ensureChild(String key, String label, String type, String command = "", String parentAttr = "") {
    String childId = "${device.id}-${key}"

    if (getChildDevice(childId))
        return

    if (type == "0") {
        def d = addChildDevice("iAqualink ToggleDevice", childId, [name: label, isComponent: true])
        d.setCommand(command, parentAttr)
    }
    else if (type == "2") {
        def d = addChildDevice("iAqualink ColorLight", childId, [name: label, isComponent: true])
        d.setAuxNumber(key.substring(4))
    }
    else if (type == "TempSensor")
        addChildDevice("MQTT Handler TempSensor", childId, [name: label, isComponent: true])
}

def ensureHeaterChild(String key, String label, String heaterType, String tempVariable) {
    String childId = "${device.id}-${key}"

    if (getChildDevice(childId))
        return

    def d = addChildDevice("iAqualink Heater", childId, [name: label, isComponent: true])
    d.setType(heaterType, tempVariable)
}

void ensureChildren() {
    debugLog("Ensuring all child devices exist.")
   if (settings.airTemp)
        addHandler("Air Temp", ""+TopicAirTemp(), 1, "TempSensor")
  /*
    ensureChild("FilterPump", "Filter Pump", "0", "set_pool_pump", "filter_pump")
    if (settings.spaMode)
        ensureChild("SpaMode", "Spa Mode", "0", "set_spa_pump", "spa_mode")
    if (settings.airTemp)
        ensureChild("AirTemp", "Air Temp", "TempSensor")
    if (settings.poolTemp)
        ensureChild("PoolTemp", "Pool Temp", "TempSensor")
    if (settings.spaTemp)
        ensureChild("SpaTemp", "Spa Temp", "TempSensor")
    if (settings.poolHeater)
        ensureHeaterChild("PoolHeater", "Pool Heater", "pool", "temp2")
    if (settings.spaHeater)
        ensureHeaterChild("SpaHeater", "Spa Heater", "spa", "temp1")
    if (settings.solarHeater)
        ensureChild("SolarHeater", "Solar Heater", "0", "set_solar_heater", "solar_heater")

    if (settings.numAux > 0 || settings.auxEA) {
        devices = getDevices()
        for (int i = 1; i <= settings.numAux; i++) {
            def device = devices[i+2]["aux_${i}"]
            ensureChild("AUX_${i}" as String, device[1].label as String, device[3].type as String, "set_aux_${i}" as String, "aux${i}")
        }

        if (settings.auxEA) {
            def device = devices[10].aux_EA
            ensureChild("AUX_EA", device[1].label as String, device[3].type as String, "set_aux_8","aux8")
        }
    }
    */
    /*
    if (settings.numOneTouch > 0) {
        onetouch = getOneTouch()
        for (int i = 1; i <= settings.numOneTouch; i++) {
            String key = "OneTouch_${i}"
            def ot = onetouch[i+1]["onetouch_${i}"]
            if (ot[0].status == "0")
                warnLog("Skipping ${key} since it does not seem to be setup on the iAqualink.")
            else
                ensureChild(key, ot[2].label as String, "0", "set_onetouch_${i}", "onetouch${i}")
        }
    }
    */
}

void autoUpdate() {
    /*
    try {
        updateStates()
    }
    catch (Exception e) {
        errorLog("Automatic status update failed...will continue to try after configured interval (${settings.autoUpdateInterval} mins). Error: ${e.message}")
    }

    if (settings.autoUpdateInterval > 0)
        runIn(settings.autoUpdateInterval * 60, autoUpdate)
    */
}

void updateParentToggleState(String key, String status) {
    sendEvent(name: key, value: (status == "1" ? "on" : "off"))
}

String convertHeaterState(Integer status) {
    switch (status) {
        case 0:
            return "off"
        case 1:
            return "heating"
        case 3:
            return "enabled"
    }
    return null
}

def convertTemperature(String temp) {
    debugLog("passed temp to convert: "+temp)
    //Integer temperature = (temp != "" ? temp as Integer : 0)
    Float temp2Change = 0.00
    if (temp != "")
    {
        temp2Change = Float.valueOf(temp)
    }
    else
    {
      //temp2Change = 0.00 
      warnLog("empty temp passed to convert; defaulting to zero")
    }
    def myAqualinkScale = device.currentValue("aqualinkd_temp_scale") as String
    def myConfiguredUnit = device.currentValue("config_temp_scale") as String

    if (myAqualinkScale == myConfiguredUnit)
        return temp2Change.round(2) 
    else if (myAqualinkScale == "F")
        return fahrenheitToCelsius(temp2Change).round(2)  
    else // if (iaqualinkScale == "C")
        return celsiusToFahrenheit(temp2Change).round(2) 
}

void updateParentTemperatureState(String key, String temp) {
    sendEvent(name: key, value: convertTemperature(temp))
}

void updateChildState(String key, String status, String label = "") {
    def child = getChildDevice("${device.id}-${key}")
    if (child) {
        debugLog("Child Device '${label == "" ? key : label}' has status '${status}'")
        child.updateState(status, label)
    }
}

void updateChildHeater(String key, String status, String freezeProtect, Integer current, Integer target, String configTempScale, String iaqualinkTempScale) {
    def child = getChildDevice("${device.id}-${key}")
    if (!child) return

    debugLog("Update '${key}': status=${status}, freeze=${freezeProtect}, current=${current}, target=${target}")
    child.updateState(status, freezeProtect, current, target, configTempScale, aqualinkdTempScale)
}

void updateStates() {
    /*
    if (!ensureLogin()) {
        errorLog("Unable to update device states due to failed login.")
        return
    }
    */

    infoLog("Updating all device states...TBD")

    /*
    def home = getHome()

    updateChildState("FilterPump", home[12].pool_pump as String, "Filter Pump")
    updateParentToggleState("filter_pump", home[12].pool_pump as String)
    if (settings.spaMode) {
        updateChildState("SpaMode", home[11].spa_pump as String, "Spa Mode")
        updateParentToggleState("spa_mode", home[11].spa_mode as String)
    }

    if (settings.numAux > 0 || settings.auxEA) {
        devices = getDevices()
        for (int i = 1; i <= settings.numAux; i++) {
            def device = devices[i+2]["aux_${i}"]
            updateChildState("AUX_${i}" as String, device[0].state as String, device[1].label as String)
            updateParentToggleState("aux${i}" as String, device[0].state as String)
            sendEvent(name: "aux${i}_label", value: device[1].label)
        }

        if (settings.auxEA) {
            def device = devices[10].aux_EA
            updateChildState("AUX_EA", device[0].state as String, device[1].label as String)
            updateParentToggleState("aux8" as String, device[0].state as String)
            sendEvent(name: "aux8_label", value: device[1].label)
        }
    }
    */
    /*
    if (settings.numOneTouch > 0) {
        onetouch = getOneTouch()
        for (int i = 1; i <= settings.numOneTouch; i++) {
            String key = "OneTouch_${i}"
            def ot = onetouch[i+1]["onetouch_${i}"]
            updateChildState(key, ot[1].state as String, ot[2].label as String)
            updateParentToggleState("onetouch${i}" as String, ot[1].state as String)
            sendEvent(name: "onetouch${i}_label", value: ot[2].label)
        }
    }
    */

    // we'd rather do this in ensureChildren which is only called when preferences change,
    // but currently that path doesn't actually fetch the Home screen so I'd rather the
    // extra event here than the extra HTTP pull all of those times
    // This also lets it update more quickly if the hub's setting were changed.

    String tTempScale = (settings.tempUnit == "hub" ? getTemperatureScale() : settings.tempUnit) as String
    sendEvent(name: "config_temp_scale", value: tTempScale)

    // mqtt data defaults to celsius
    String tAqualinkTempScale = "C"
    sendEvent(name: "aqualinkd_temp_scale", value: tAqualinkTempScale)
/*
    sendEvent(name: "online", value: (home[0].status == "Online" ? "on" : "off"))
    updateParentToggleState("solar_heater", home[15].solar_heater as String)
    sendEvent(name: "spa_salinity", value: home[16].pool_salinity)
    sendEvent(name: "pool_salinity", value: home[17].pool_salinity)
    sendEvent(name: "orp", value: home[18].orp)
    sendEvent(name: "ph", value: home[19].ph)

    String freezeProtect = (home[10].freeze_protection as String) == "1" ? "on" : "off"
    sendEvent(name: "freeze_protection", value: freezeProtect)

    String spaHeaterState = convertHeaterState(home[13].spa_heater as Integer)
    String poolHeaterState = convertHeaterState(home[14].pool_heater as Integer)
    sendEvent(name: "spa_heater", value: spaHeaterState)
    sendEvent(name: "pool_heater", value: poolHeaterState)


    Integer targetPoolTemp = convertTemperature(home[8].pool_set_point as String)
    Integer targetSpaTemp = convertTemperature(home[7].spa_set_point as String)
    sendEvent(name: "pool_set_point", value: targetPoolTemp, unit: tempScale)
    sendEvent(name: "spa_set_point", value: targetSpaTemp, unit: tempScale)

    Integer currentAirTemp = convertTemperature(home[6].air_temp as String)
    Integer currentPoolTemp = convertTemperature(home[5].pool_temp as String)
    Integer currentSpaTemp = convertTemperature(home[4].spa_temp as String)
    sendEvent(name: "air_temp", value: currentAirTemp, unit: tempScale)
    sendEvent(name: "pool_temp", value: currentPoolTemp, unit: tempScale)
    sendEvent(name: "spa_temp", value: currentSpaTemp, unit: tempScale)
    updateChildState("AirTemp", currentAirTemp as String)
    updateChildState("PoolTemp", currentPoolTemp as String)
    updateChildState("SpaTemp", currentSpaTemp as String)

    if (settings.poolHeater)
        updateChildHeater("PoolHeater", poolHeaterState, freezeProtect, currentPoolTemp, targetPoolTemp, tempScale, iaqualinkTempScale)
    if (settings.spaHeater)
        updateChildHeater("SpaHeater", spaHeaterState, freezeProtect, currentSpaTemp, targetSpaTemp, tempScale, iaqualinkTempScale)
    */
}
