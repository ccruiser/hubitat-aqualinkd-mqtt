// Copyright (C) 2023 Casey Cruise
//
// See LICENSE.txt for licensing terms of this software.

// Child device type used by MQTT Connection; this is a modification of the orginal child device
// This device adds "switching" feature sets for a chile with with switches

// Installation:

// Copy this file and mqtt-connection-driver.groovy as "New Drivers" in the user
// driver area of the Hubitat UI.

// Usage:

// This device type is not designed to be used on its own. Devices of this type
// are created as "component" devices of a MQTT Connection device using the
// latter's Add Handler custom command. Once created, you can refer to the
// "message" custom attribute of MQTT Handler devices in RM triggers and
// actions.

metadata {

  definition (name: "MQTT Handler Switch",
              namespace: "ccruiser",
              author: "Casey Cruise",
              importUrl: "https://raw.githubusercontent.com/ccruiser/hubitat-aqualinkd-mqtt/main/Drivers/mqtt-handler-driver-switch.groovy") {

    // Declare a capabilities, this is a switch device
    capability "Initialize"
    capability "Switch"

    // Payload of incoming message.
    attribute "payload", "STRING"

  }
      preferences {
                input(name: "SwitchOnTopic", type: "string", title:"Switch 'On' Topic", description: "Enter the topic used when 'on' is invoked", required: true)
                input(name: "SwitchOffTopic", type: "string", title:"Switch 'Off' Topic", description: "Enter the topic used when 'off' is invoked", required: true)
  }

}

// Standard driver life-cycle callback.
def installed() {

  initialize()

}

// Standard driver life-cycle callback.
def updated() {

  initialize()

}

// Standard Initialize life-cycle command.
def initialize() {

  // nothing to do here

}

def on() {
	push(1)
}

def off() {
	push(0)
}

def push(int pstate) {
    //set values for events
    def valState = "off"
    def topic2Set = "SwitchOffTopic"
    if (pstate) {
       valState = "on"
       topic2Set = "SwitchOnTopic"
    }
    // Send Event and Run Command to Publish
    sendEvent(name: "switch", value: valState, isStateChange: true)
    runCmd(topic2Set , pstate)

}

//Take action on push event
def runCmd(String varTopic, int valToSet) {
    log.info( "Publishing to topic: "+varTopic+" value: "+valToSet )
    parent.publish(varTopic, valToSet, 1, "false")  
}
