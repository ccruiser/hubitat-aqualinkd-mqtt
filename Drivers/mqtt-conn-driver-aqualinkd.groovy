 // Copyright (C) Casey Cruise 2023

// See LICENSE.txt for licensing terms of this software.

// Hubitat device driver that wraps the internal MQTT client interface

// Motivation:

// - Create a simple AqualinkD MQTT Interface

// - Remove the need for API requests or other integrations 

// Note that this driver and its MQTT Handler companion driver expose only the
// most basic MQTT features. They do not support complex MQTT-based
// functionality such as the homie protocol for automatic device discovery.
// There is a much richer MQTT Client app under development by another member of
// the community which would be a far better choice for anyone who wants such
// features. (See <https://community.hubitat.com/t/mqtt-client/3808/34>)

// This driver's ambitions are far more modest. It is simply to provide support
// for basic MQTT messaging in Rule Machine, coming as close as possible to its
// direct support for HTTP messaging as triggers and actions, within the highly
// constrained limits of what it is possible to do within Rule Machine at all.

// Features:

// - Inherits support for both secure (tcp://) and encrypted (ssl://)
//   connections with or without password protection from the underlying MQTT
//   client interface
//
// - Publish messages to MQTT topics using "run custom action" in Rule Machine
//   to invoke this driver's `publish(topic, message, qos, retain)` method
//
//   - Note that due to deficiencies in Rule Machine you must select the
//     "Actuator" capability when starting the process of choosing a particular
//     device whose method is to be invoked by "run custom action" in order to
//     see your MQTT Connection device offered as an option among the devices
//     whose custom action you wish to invoke.
//
// - Send child device events on receipt of messages on subscribed topics to
//   support "custom attribute" triggers and actions in Rule Machine
//
//    - When using a "custom attribute" action, you can access the actual
//      message payload using the built-in `%value%` variable

// Installation:

// Copy this file and mqtt-handler-driver.groovy as "New Drivers" in the "Driver
// Code" area of the Hubitat UI.

// Usage:

// 1. Create a virtual "MQTT Connection" device
//
// 2. Set the broker address and, optionally, user name and password
//
//    a. Use tcp:// prefix for a standard, unencrypted connection
//
//    b. Use ssl:// prefix for an encrypted (TLS) connection
//
// 3. Click the "Save Preferences" button
//
// 4. Use "Run custom action" in RM to invoke "publish"
//
// 5. Use "Add Handler" to add MQTT Handler child devices for specific topics
//
// 6. Use "Custom Attribute" triggers and actions to access received messages in
//    RM
/*
 * Known Issue(s) & Gaps:
 * 1) Missing Dynamic lists for aqualink buttons / inputs
 * 2) Missing temperature control handler
 * 3) Needed other features (only switch is supported)
 * 4) Missing timer controls ? 
 * 
 * Version Control:
 * 0.2.0 - added new device handler type; added driver setup 
 *       - added last published attrribute data
 *       - added preferences show / hide options
 *       - added switch logic to sync payload 
 * 0.1.0 - Initial version based on mq-connection-driver-aqualinkd
 * 
 * Thank you(s):
 * Kirk Rader for orginal code base and foundation of MQTT Setup
 *
 */

// Returns the driver name
def DriverName(){
    return "MQTT Connection 2"
}

// Returns the driver version
def DriverVersion(){
    return "0.2.0"
}

metadata {

  definition (
    name: "MQTT Connection 2",
    namespace: "ccruiser",
    author: "Casey Cruise",
    importUrl: "https://raw.githubusercontent.com/ccruiser/hubitat-aqualinkd-mqtt/main/Drivers/mqtt-conn-driver-aqualinkd.groovy") {

    // Enable the "initialize" life-cycle method.
    capability "Initialize"

    // Include a capability that is integrated with Rule Machine
    capability "Actuator"

    // Define the Current Driver attributes 
    attribute "Driver Name", "string" // Identifies the driver being used for update purposes
		attribute "Driver Version", "string" // Handles version for driver

    // Send a MQTT message.
    command "publish", [
      [name: "topic", type: "STRING"],
      [name: "payload", type: "STRING"],
      [name: "qos", type: "INTEGER"],
      [name: "retain", type: "STRING"]
    ]

    // Add a child MQTT Handler device.
    command "addHandler", [
      [name: "name", type: "STRING"],
      [name: "topic", type: "STRING"],
      [name: "qos", type: "INTEGER"],
      [name: "hType", type: "STRING", options: [ "Listener", "Switch" ], defaultValue: "Listener"]
    ]

    // Remove the specified child MQTT Handler device.
    command "deleteHandler", [
      [name: "device network id", type: "STRING"]
    ]

    // Change the topic handled by a given child device.
    command "replaceTopic", [
      [name: "topic", type: "STRING"],
      [name: "qos", type: "INTEGER"],
      [name: "device network id", type: "STRING"]
    ]

    command "connect"

    command "disconnect"

    // State of the connection to the MQTT broker ("connected" or
    // "disconnected").
    attribute "Connection Status", "STRING"
    attribute "Connection Last Event", "STRING"


  }

  preferences {
    if( ShowAllPreferences || ShowAllPreferences == null ){ // Show the preferences options

      input(
        name: "broker",
        type: "text",
        title: "Broker URL",
        description: "use tcp:// or ssl:// prefix",
        required: true,
        displayDuringSetup: true
      )

      input(
        name: "clientId",
        type: "text",
        title: "Client Id",
        description: "MQTT client id",
        required: true,
        displayDuringSetup: true,
        defaultValue: UUID.randomUUID().toString()
      )

      input(
        name: "username",
        type: "text",
        title: "Username",
        description: "(optional)",
        required: false,
        displayDuringSetup: true
      )

      input(
        name: "password",
        type: "password",
        title: "Password",
        description: "(optional)",
        required: false,
        displayDuringSetup: true
      )

      input(
        name: "lwtTopic",
        type: "text",
        title: "LWT Topic",
        description: "LWT message topic when disconnecting from broker",
        required: true,
        displayDuringSetup: true,
        defaultValue: "hubitat/lwt"
      )

      input(
        name: "lwtMessage",
        type: "text",
        title: "LWT Message",
        description: "LWT message body when disconnecting from broker",
        required: true,
        displayDuringSetup: true,
        defaultValue: "disconnected"
      )
      input( 
        type: "bool", 
        name: "ShowAllPreferences", 
        title: "<b>Show All Preferences?</b>", 
        defaultValue: true 
      )
      /*
      input( type: "bool", 
        name: "AdvancedCommands", 
        title: "<b>Enable Advanced Commands?</b>", 
        description: "Allows Hander delete, create, update functions.", 
        defaultValue: false )
      */


    } else {
      input( type: "bool", 
      name: "ShowAllPreferences", 
      title: "<b>Show All Preferences?</b>", 
      defaultValue: true )
    }

  }
}

////////////////////////////////////////////////////////////////////////////////
// Driver life-cycle methods

// Defer full initialization until after parameters are set manually.
def installed() {

  state.handlers = [:]

  synchronized (state.handlers) {

    log.info("initializing connection for installed device")
    sendEvent(name: "Connection Status", value: "disconnected")
    sendEvent(name: "Connection Last Event", value: new Date() )

    if (settings.broker?.trim()) {

      initialize()

    }
  }
}

// (Re)initialize after parameters are changed.
def updated() {

  initialize()

}

////////////////////////////////////////////////////////////////////////////////
// Initialize capability life-cycle method

// Disconnect from the MQTT broker, if necessary, then connect.
def initialize() {

  //Set Version and Driver
  sendEvent(name: "Driver Name", value: "${ DriverName() }")
  sendEvent(name: "Driver Version", value: "${ DriverVersion() }")

  //Initialize Brokers
  synchronized (state.handlers) {

    disconnect()
    connect()
    log.info("initalization complete")

  }
}

////////////////////////////////////////////////////////////////////////////////
// Custom commands

// Connect to the MQTT broker.
def connect() {

  synchronized (state.handlers) {

    // keep trying to connect until successful
    // or some other thread indicates that this
    // attempt should cease

    state.reconnect = true
    state.handlers.notifyAll()

    while (!state.connected && state.reconnect) {

      try {

        interfaces.mqtt.connect(settings.broker, settings.clientId,
                                settings.username, settings.password,
                                lastWillTopic: settings.lwtTopic, lastWillQos: 0,
                                lastWillMessage: settings.lwtMessage)

        state.connected = true
        state.handlers.notifyAll()
        log.info "connected to " + settings.broker

        state.handlers.each { topic, subscription ->

          subscribe(topic, subscription.qos)

        }

        sendEvent(name: "Connection Status", value: "connected")
        sendEvent(name: "Connection Last Event", value: new Date() )


      } catch (e) {

        log.error "error connecting to MQTT broker: ${e}"
        sendEvent(name: "Connection Status", value: "error")
        sendEvent(name: "Connection Last Event", value: new Date() )
        state.handlers.wait(5000)

      }
    }
  }
}

// Disconnect from the MQTT broker.
def disconnect() {

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
        log.debug("MQTT attempting disconnect from "+settings.broker)
        interfaces.mqtt.disconnect()


      } catch (e) {

        log.error "error disconnecting: ${e}"

      }

      state.connected = false
      state.handlers.notifyAll()
      log.info "disconnected from " + settings.broker
      sendEvent(name: "Connection Status", value: "disconnected")
      sendEvent(name: "Connection Last Event", value: new Date() )


    }
  }
}

// Publish the given payload on the given MQTT topic.
def publish(String topic, String payload, int qos = 2,
            boolean retained = false ) {

  synchronized (state.handlers) {

    try {

      interfaces.mqtt.publish(topic, payload, qos, retained)
      log.info "published topic: "+topic+" with value: "+payload+" qos: "+qos+" retained: "+retained

    } catch (e) {

      log.error "error publishing ${payload} on topic ${topic}: ${e}"

    }
  }
}

// RM-compatible overload for publish(String, String, int, boolean).
def publish(String topic, String payload, int qos, String retained) {
  log.info("topic: "+topic+" published called with payload: "+payload)
  publish(topic, payload, qos, retained.toBoolean())

}

// Add a MQTT Handler child device subscribed to the specified topic.
def addHandler(String name, String topic, int qos, String hType) {

  synchronized (state.handlers) {
    def id = UUID.randomUUID().toString()
    log.info(" adding child device of type"+hType+" with name: "+name)
     switch( hType ){
            case "Switch":
                addChildDevice("ccruiser", "MQTT Handler Switch", id,
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

    log.error "mqttClientStatus: ${message}"
    disconnect()
    runIn (5,"connect")

  } else {

    log.info "mqttClientStatus: ${message}"

  }
}

// Callback invoked by the MQTT interface on receipt of a MQTT message.
//
// Looks up the corresponding MQTT Handler child device in state.handlers
// and forwards the message payload as an event.
def parse(String event) {

  synchronized (state.handlers) {

    def message = interfaces.mqtt.parseMessage(event)

    for (element in state.handlers) {

      if (element.key.equals(message.topic)) {

        def handler = getChildDevice(element.value.id)

        if (handler == null) {

          log.warn "parse: no child found with id ${element.value.id}"

        } else {

          log.debug("sending update from topic: "+message.topic+" for payload: "+message.payload)
          handler.sendEvent(name: "payload", value: message.payload)
          handler.sendEvent(name: "Last Published", value: new Date())
          // Check for Handler Types
          def defType = handler.currentValue("Handler Type")
          log.debug(" validating handler payload for handler action for type: "+defType)
            switch( defType ){
                  case "Switch":
                      log.debug(" setting 'switch' value per payload: "+message.payload)
                      switch (message.payload){
                        case "1":
                           handler.sendEvent(name: "switch", value: "on")
                        break
                        case "0":
                           handler.sendEvent(name: "switch", value: "off")
                        break
                        default:
                          log.warn("Unknown handler type: "+defType+" with payload "+message.payload )
                        break

                      }
                  break
                  case "Listener":
                      log.debug(" setting 'listener' value per payload: "+message.payload)
                  break
                  default:
                    log.warn("Unknown handler type: "+defType )
                  break
          }

        }
      }
    }
  }
}

////////////////////////////////////////////////////////////////////////////////
// Helper functions

// Subscribe to the specified topic at the specified quality of service setting.
def subscribe(String topic, int qos = 2) {

  synchronized (state.handlers) {

    try {

      interfaces.mqtt.subscribe(topic, qos)
      log.info "subscribed to ${topic}"

    } catch (e) {

      log.error "error subscribing to topic ${topic} (qos=${qos})"

    }
  }
}

// Unsubscribe from the specified topic.
def unsubscribe(String topic) {

  synchronized (state.handlers) {

    try {

      interfaces.mqtt.unsubscribe(topic)
      log.info "unsubscribed from ${topic}"

    } catch (e) {

      log.error "error unsubscribing from topic ${topic}"

    }
  }
}

// Unsubscribe from the topic currently handled by the child device with the given id.
def unsubscribeHandler(String id) {

  synchronized (state.handlers) {

    // note: the version of groovy in hubitat seems to be missing Map.removeAll()

    def topic = null

    for (element in state.handlers) {

      if (element.value.id == id) {

        topic = element.key
        break

      }
    }

    if (topic != null) {
      log.debug("unsubscribing events from topic: "+topic+"")
      state.handlers.remove(topic)
      unsubscribe(topic)

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
      log.debug("subscribing events for child device: "+handler+" for topic "+topic)
      handler.sendEvent(name: "topic", value: topic)
      /*
      if (hType != "" ) {
          handler.sendEvent(name: "Handler Type", value: hType)
      } 
      */

    } else {

      log.warn "subscribeHandler: no child device found with id ${id}"

    }
  }
}
