#include <Wire.h>
#include <CurieBLE.h> // Curie BLE lib

byte sensorVersion = 0; //Keeps track of whether we have an AS7262 or AS7263
#define SENSORTYPE_AS7262 0x3E
#define SENSORTYPE_AS7263 0x3F

// See the following for generating UUIDs:
// https://www.uuidgenerator.net/

#define LOW_UUID        "4fafc201-1fb5-459e-8fcc-000000000001"
#define HIGH_UUID        "4fafc201-1fb5-459e-8fcc-000000000002"

#define LOW_SRV_UUID "beb5483e-36e1-4688-b7f5-000000000001"
#define HIGH_SRV_UUID "beb5483e-36e1-4688-b7f5-000000000002"


//https://www.bluetooth.com/specifications/gatt/services
BLEService sensorSvc(LOW_UUID); 
BLEService hsensorSvc(HIGH_UUID); 

//https://www.bluetooth.com/specifications/gatt/characteristics
BLECharacteristic LowChar(LOW_SRV_UUID, BLERead,75);

BLECharacteristic HighChar(HIGH_SRV_UUID, BLERead,75);

long previousMillis = 0;  // last time the battery level was checked, in ms

void setup()
{
  Wire.begin();
  Serial.begin(115200);
    Serial.print("Booting");

  // begin initialization
  BLE.begin();
  BLE.setLocalName("Luci");
  
  BLE.setAdvertisedServiceUuid(LOW_UUID);
  BLE.setAdvertisedService(sensorSvc);

  BLE.setAdvertisedServiceUuid(HIGH_UUID);
  BLE.setAdvertisedService(hsensorSvc);
  
  sensorSvc.addCharacteristic(LowChar);

  hsensorSvc.addCharacteristic(HighChar);

  
  BLE.addService(sensorSvc);
  BLE.addService(hsensorSvc);
  // sensorChar.setValue(sensorData, 5);
  BLE.advertise();
  
  Serial.print("init finished");

}

void loop()
{
  // listen for BLE peripherals to connect:
  BLEDevice central = BLE.central();

  // if a central is connected to peripheral:
  if (central) {
    Serial.print("Connected to central: ");
    // print the central's MAC address:
    Serial.println(central.address());
    // turn on the LED to indicate the connection:
    //digitalWrite(13, HIGH);

    // sensor every 200ms
    // as long as the central is still connected:
    while (central.connected()) {
      long currentMillis = millis();
      // if 200ms have passed, check the battery level:
      if (currentMillis - previousMillis >= 4000) {
        previousMillis = currentMillis;
        readSensors();
      }
    }


    Serial.print("Disconnected from central: ");
    Serial.println(central.address());
  }

  
}

void readSensors() {

  
  // communicate to the i2c multiplexer (address = 0x70)
  Wire.beginTransmission(0x70);
  Wire.write(0x05); // select channel 0
  Wire.read();
  Wire.endTransmission();   
 // delay(50); 

  sensorVersion = as726xSetup();
    Serial.println(String(sensorVersion));

  if (sensorVersion == 0)
  {
    Serial.println("Sensor failed to respond. Check wiring.");
    while (1); //Freeze!
  }
  //delay(50); 
  takeMeasurements(); //No LED - easier on your eyes
  //takeMeasurementsWithBulb(); //Use LED - bright white LED

  float tempF = getTemperatureF();
  
  if(sensorVersion == SENSORTYPE_AS7262)
  {
      Serial.println("AS7262 online!");

    //Visible readings
    Serial.print(" Reading: V[");
    Serial.print(getCalibratedViolet(), 2);
    Serial.print("] B[");
    Serial.print(getCalibratedBlue(), 2);
    Serial.print("] G[");
    Serial.print(getCalibratedGreen(), 2);
    Serial.print("] Y[");
    Serial.print(getCalibratedYellow(), 2);
    Serial.print("] O[");
    Serial.print(getCalibratedOrange(), 2);
    Serial.print("] R[");
    Serial.print(getCalibratedRed(), 2);
  Serial.print("] tempF[");
  Serial.print(tempF, 1);
  Serial.print("]");
  Serial.println();

int vioLen = 0;
// Send string
String VioString = String(getCalibratedViolet())+":"+String(getCalibratedBlue())+":"+String(getCalibratedGreen())+":"+String(getCalibratedYellow())+":"+String(getCalibratedOrange())+":"+String(getCalibratedRed());
// everything wants to know how long the buf should be
vioLen = VioString.length()+1;
//delay(10);
// char buf
uint8_t viobuf[vioLen];
// Convert send string to char array
VioString.getBytes(viobuf, vioLen);
// Send the char array via BLE
LowChar.setValue(viobuf, vioLen);   

  }
    Serial.println("Switching");

 //softReset();
 
  //delay(2000); 


clearDataAvailable();

  // communicate to the i2c multiplexer (address = 0x70)
  Wire.beginTransmission(0x70);
  Wire.write(0x04); // select channel 1
  Wire.endTransmission();   
  //delay(50); 



  sensorVersion = as726xSetup();
  Serial.println(String(sensorVersion));
  if (sensorVersion == 0)
  {
    Serial.println("Sensor failed to respond. Check wiring.");
    while (1); //Freeze!
  }

//  delay(50); 
  takeMeasurements(); //No LED - easier on your eyes

  
  if(sensorVersion == SENSORTYPE_AS7263)
  {
      Serial.println("AS7263 online!");

    //Near IR readings
    Serial.print(" Reading: R[");
    Serial.print(getCalibratedR(), 2);
    Serial.print("] S[");
    Serial.print(getCalibratedS(), 2);
    Serial.print("] T[");
    Serial.print(getCalibratedT(), 2);
    Serial.print("] U[");
    Serial.print(getCalibratedU(), 2);
    Serial.print("] V[");
    Serial.print(getCalibratedV(), 2);
    Serial.print("] W[");
    Serial.print(getCalibratedW(), 2);
  }

  Serial.print("] tempF[");
  Serial.print(tempF, 1);
  Serial.print("]");

  Serial.println();


int vLen = 0;
// Send string
String VString = String(getCalibratedR())+":"+String(getCalibratedS())+":"+String(getCalibratedT())+":"+String(getCalibratedU())+":"+String(getCalibratedV())+":"+String(getCalibratedW())   ;
// everything wants to know how long the buf should be
vLen = VString.length()+1;
//delay(10);
// char buf
uint8_t vbuf[vLen];
// Convert send string to char array
VString.getBytes(vbuf, vLen);
// Send the char array via BLE
HighChar.setValue(vbuf, vLen);   

 //softReset();
 
 //delay(2000); 
    Serial.println("------------------------------------------------------------------------------------");

}
