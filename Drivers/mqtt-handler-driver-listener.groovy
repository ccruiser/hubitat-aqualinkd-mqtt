// Copyright (C) Casey Cruise 2023

// See LICENSE.txt for licensing terms of this software.

// Child device type used by MQTT Connection for listening to message events (no action)

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
 * 0.2.0 - added topic attribute
 * 0.1.0 - Initial version based on mq-handler-driver
 * 
 * Thank you(s):
 * Kirk Rader for orginal code base and foundation of MQTT Setup
 */


metadata {

  definition (name: "MQTT Handler Listener",
              namespace: "ccruise",
              author: "Casey Cruise",
              importUrl: "https://raw.githubusercontent.com/ccruiser/hubitat-aqualinkd-mqtt/main/Drivers/mqtt-handler-driver-listener.groovy") {

    // Declare a capability that is compatible with RM
    capability "Initialize"

    // Payload of incoming message.
    attribute "payload", "STRING"
    attribute "topic", "STRING"


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
