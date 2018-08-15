/*
  Using the AS726x Spectral Sensor
  By: Nathan Seidle
  SparkFun Electronics
  Date: March 8th, 2017
  License: This code is public domain but you buy me a beer if you use this and we meet someday (Beerware license).

  Feel like supporting our work? Buy a board from SparkFun!

  Read the calibrated sensor values. These are more accurate but you have to deal with floating point numbers.
  Calibrated Violet, Blue, Green, Yellow, Orange, and Red data from the AS7262 (Visible)
  Calibrated R, S, T, U, V, W data from the AS7263 (NIR)

  The AS726x Qwiic board can be configured to communicate over I2C (default) or serial. This example
  assumes we are communicating over I2C. See schematic for jumpers to change to serial and datasheet
  for the AT command interface.

  Hardware Connections:
  Attach the Qwiic Shield to your Arduino/Photon/ESP32 or other
  Plug the AS726x onto the shield
  Serial.print it out at 115200 baud to serial monitor.

  Available:
  void takeMeasurements()
  void takeMeasurementsWithBulb()
  int getViolet() Blue() Green() Yellow() Orange() Red()
  float getCalibratedViolet(), Blue, Green, Yellow, Orange, Red
  void setMeasurementMode(byte mode)
  boolean dataAvailable()
  boolean as726xSetup()
  byte getTemperature()
  byte getTemperatureF()
  void setIndicatorCurrent(byte)
  void enableIndicator()
  void disableIndicator()
  void setBulbCurrent(byte)
  void enableBulb()
  void disableBulb()
  void softReset()
  setGain(byte gain)
  setIntegrationTime(byte integrationValue)
  enableInterrupt()
  disableInterrupt()

  To Write:

*/

#include <Wire.h>

byte sensorVersion = 0; //Keeps track of whether we have an AS7262 or AS7263
#define SENSORTYPE_AS7262 0x3E
#define SENSORTYPE_AS7263 0x3F


/*
#include <BLE.h>
#include <BLEUtils.h>
#include <BLEServer.h>

BLE ble;

// See the following for generating UUIDs:
// https://www.uuidgenerator.net/

#define SERVICE_UUID        "4fafc201-1fb5-459e-8fcc-c5c9c331914b"

#define VIOLET_UUID "beb5483e-36e1-4688-b7f5-000000000001"
#define BLUE_UUID "beb5483e-36e1-4688-b7f5-000000000002"
#define GREEN_UUID "beb5483e-36e1-4688-b7f5-000000000003"
#define YELLOW_UUID "beb5483e-36e1-4688-b7f5-000000000004"
#define ORANGE_UUID "beb5483e-36e1-4688-b7f5-000000000005"
#define RED_UUID "beb5483e-36e1-4688-b7f5-000000000006"

BLECharacteristic *VioletCharacteristic;
BLECharacteristic *BlueCharacteristic;
BLECharacteristic *GreenCharacteristic;
BLECharacteristic *YellowCharacteristic;
BLECharacteristic *OrangeCharacteristic;
BLECharacteristic *RedCharacteristic;
*/
void setup()
{
  Wire.begin();

 
  Serial.begin(115200);

Serial.println("started");

  
/*
  Serial.println("Starting BLE work!");

  ble.initServer("Luci");

  BLEServer *pServer = new BLEServer();
  BLEService *pService = pServer->createService(BLEUUID(SERVICE_UUID));
  
//1-----------------------------------------------------------------------
  VioletCharacteristic = pService->createCharacteristic(
                                         BLEUUID(VIOLET_UUID),
                                         BLECharacteristic::PROPERTY_READ 
                                       );
  VioletCharacteristic->setValue("");
//2-----------------------------------------------------------------------
  BlueCharacteristic = pService->createCharacteristic(
                                         BLEUUID(BLUE_UUID),
                                         BLECharacteristic::PROPERTY_READ 
                                       );
  BlueCharacteristic->setValue("");
//3-----------------------------------------------------------------------
  GreenCharacteristic = pService->createCharacteristic(
                                         BLEUUID(GREEN_UUID),
                                         BLECharacteristic::PROPERTY_READ 
                                       );
  GreenCharacteristic->setValue("");
//4-----------------------------------------------------------------------
  YellowCharacteristic = pService->createCharacteristic(
                                         BLEUUID(YELLOW_UUID),
                                         BLECharacteristic::PROPERTY_READ 
                                       );
  YellowCharacteristic->setValue("");
//5-----------------------------------------------------------------------
  OrangeCharacteristic = pService->createCharacteristic(
                                         BLEUUID(ORANGE_UUID),
                                         BLECharacteristic::PROPERTY_READ 
                                       );
  OrangeCharacteristic->setValue("");
//6-----------------------------------------------------------------------
  RedCharacteristic = pService->createCharacteristic(
                                         BLEUUID(RED_UUID),
                                         BLECharacteristic::PROPERTY_READ 
                                       );
  RedCharacteristic->setValue("");
//-----------------------------------------------------------------------

  pService->start();
  BLEAdvertising *pAdvertising = pServer->getAdvertising();
  pAdvertising->start();

  pinMode(16, OUTPUT);
  digitalWrite(16, HIGH);   // turn the LED on (HIGH is the voltage level)
*/
}


void loop()
{

/*
  // communicate to the i2c multiplexer (address = 0x70)
  Wire.beginTransmission(0x70);
  //Wire.write(0x04); // select channel 0
  Wire.write(0x05); // select channel 1

  Wire.endTransmission();   
    delay(50); 
*/

Serial.println("Running");

  sensorVersion = as726xSetup();

  Serial.println("setup");

  if (sensorVersion == 0)
  {
    Serial.println("Sensor failed to respond. Check wiring.");
    while (1); //Freeze!
  }

  Serial.println("measure");

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


/*
    float Blue = getCalibratedBlue();
    float Green = getCalibratedGreen();
    float Yellow = getCalibratedYellow();
    float Orange = getCalibratedOrange();
    float Violet = getCalibratedViolet();
    float Red = getCalibratedRed();

    String RED = String(Red);
    String BLUE = String(Blue);
    String GREEN = String(GREEN);
    String YELLOW = String(Yellow);
    String ORANGE = String(Orange);
    String VIOLET = String(Violet);

    char str[10];

    VIOLET.toCharArray(str, 10);
    VioletCharacteristic->setValue(str);
    
    BLUE.toCharArray(str, 10);
    BlueCharacteristic->setValue(str);

    GREEN.toCharArray(str, 10);
    GreenCharacteristic->setValue(str);

    YELLOW.toCharArray(str, 10);
    YellowCharacteristic->setValue(str);

    ORANGE.toCharArray(str, 10);
    OrangeCharacteristic->setValue(str);
    
    RED.toCharArray(str, 10);
    RedCharacteristic->setValue(str);
    

*/
  }

/*
    delay(10); 

  // communicate to the i2c multiplexer (address = 0x70)
  Wire.beginTransmission(0x70);
  //Wire.write(0x04); // select channel 0
  Wire.write(0x05); // select channel 1

  Wire.endTransmission();   
    delay(50); 
*/



  sensorVersion = as726xSetup();
  if (sensorVersion == 0)
  {
    Serial.println("Sensor failed to respond. Check wiring.");
    while (1); //Freeze!
  }


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





  
}
