#include <Wire.h>
//#include <CurieBLE.h> // Curie BLE lib

byte sensorVersion = 0; //Keeps track of whether we have an AS7262 or AS7263
#define SENSORTYPE_AS7262 0x3E
#define SENSORTYPE_AS7263 0x3F


//#include "BLEUtils.h"
//#include "BLEServer.h"

#include <esp_log.h>
#include <string>
#include <sys/time.h>
#include <sstream>

//#include "BLEDevice.h"

#include "sdkconfig.h"
#include <base64.h>


#include <BLEDevice.h>
#include <BLEUtils.h>
#include <BLEServer.h>


// See the following for generating UUIDs:
// https://www.uuidgenerator.net/

#define LOW_CHAR_UUID        "4fafc201-1fb5-459e-8fcc-000000000001"
//#define HIGH_CHAR_UUID        "4fafc201-1fb5-459e-8fcc-000000000002"

#define LOW_UUID "beb5483e-36e1-4688-b7f5-000000000001"
#define HIGH_UUID "beb5483e-36e1-4688-b7f5-000000000002"

/*
//https://www.bluetooth.com/specifications/gatt/services
BLEService sensorSvc(LOW_UUID); 
BLEService hsensorSvc(HIGH_UUID); 

//https://www.bluetooth.com/specifications/gatt/characteristics
BLECharacteristic LowChar(LOW_SRV_UUID, BLERead,75);

BLECharacteristic HighChar(HIGH_SRV_UUID, BLERead,75);

*/

long previousMillis = 0;  // last time the battery level was checked, in ms

BLECharacteristic *pCharacteristic;
//BLECharacteristic *highCharacteristic;

//char vsendstr[23];

static uint8_t  SERVICE_UUID_BIN[] = {0x4f, 0xaf, 0xc2, 0x01, 0x1f, 0xb5, 0x45, 0x9e, 0x8f, 0xcc, 0x00, 0x00, 0x00, 0x00, 0x00, 0xaa};

char vsendstr[23];
float readings[12];

int snum = 0;

class MyCallbackHandler: public BLECharacteristicCallbacks {
  
  void onRead(BLECharacteristic *pCharacteristic) {
    String SEND = String(snum)+":"+String(readings[snum]);
    SEND.toCharArray(vsendstr, 23);
    pCharacteristic->setValue(vsendstr);
    ++snum;
    if(snum == 12){
      Serial.print("12");
      snum = 0;
    }
  
  }
};

/*
class HighCallbackHandler: public BLECharacteristicCallbacks {
  void onRead(BLECharacteristic *highCharacteristic) {
    highCharacteristic->setValue(hsendstr);
  }
};
*/

void setup()
{
pinMode(12, OUTPUT);  
Wire.begin();
Serial.begin(115200);
  Serial.print("Booting");

 
BLEDevice::init("Luciscan");
BLEDevice::setMTU(80);
BLEDevice::setPower(ESP_PWR_LVL_P1);

BLEServer *pServer = BLEDevice::createServer();
BLEService *pService = pServer->createService(BLEUUID(SERVICE_UUID_BIN, 16, true));

BLECharacteristic *pCharacteristic = pService->createCharacteristic(
BLEUUID(LOW_CHAR_UUID),
BLECharacteristic::PROPERTY_READ
);

  pCharacteristic->setCallbacks(new MyCallbackHandler());
  pCharacteristic->setValue("Hello World");

/*
  BLECharacteristic *highCharacteristic = pService->createCharacteristic(
    BLEUUID(HIGH_CHAR_UUID),
    BLECharacteristic::PROPERTY_READ
  );
  highCharacteristic->setCallbacks(new HighCallbackHandler());
  highCharacteristic->setValue("Hello World");
*/
  pService->start();
  BLEAdvertising *pAdvertising = pServer->getAdvertising();

  pAdvertising->start();
 
 // Serial.print("init finished");
  digitalWrite(12, LOW);    // turn the LED off by making the voltage LOW
  delay(10); 




readSensors();

}

void loop()
{
 
      if(snum == 12){
        readSensors();
    }
  
}

void readSensors() {
  // communicate to the i2c multiplexer (address = 0x70)
  Wire.beginTransmission(0x70);
  Wire.write(0x05); // select channel 0
  //Wire.read();
  //Serial.println(Wire.read());
  Wire.endTransmission();   
  delay(10); 
  sensorVersion = as726xSetup();
   // Serial.println(String(sensorVersion));
disableBulb();
disableIndicator();
  if (sensorVersion == 0)
  {
    //Serial.println("Sensor failed to respond. Check wiring.");
    while (1); //Freeze!
  }
  //delay(50); 
  takeMeasurements(); //No LED - easier on your eyes
  //takeMeasurementsWithBulb(); //Use LED - bright white LED

  float tempF = getTemperatureF();
  
  if(sensorVersion == SENSORTYPE_AS7262)
  {
      //Serial.println("AS7262 online!");
    /*
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

String STR_LOW = base64::encode(String(getCalibratedViolet()*0.01)+":"+String(getCalibratedBlue()*0.01)+":"+String(getCalibratedGreen()*0.01)+":"+String(getCalibratedYellow()*0.01)+":"+String(getCalibratedOrange()*0.01)+":"+String(getCalibratedRed()*0.01)+":L");
  delay(10);
STR_LOW.toCharArray(vsendstr, 45);


*/   

//String STR_LOW = base64::encode("1:"+String(getCalibratedViolet()*0.01));
//  delay(500);
//STR_LOW.toCharArray(vsendstr, 23);

readings[0] = getCalibratedViolet()*0.01; 
readings[1] = getCalibratedBlue()*0.01; 
readings[2] = getCalibratedGreen()*0.01; 
readings[3] = getCalibratedYellow()*0.01; 
readings[4] = getCalibratedOrange()*0.01; 
readings[5] = getCalibratedRed()*0.01; 

//Serial.println(base64::encode(vstr)); 
  }

  
 //softReset();
  delay(10);
//----------------------------------------------------------------------------------------------------------------------
//Serial.println("switching");

  // communicate to the i2c multiplexer (address = 0x70)
  Wire.beginTransmission(0x70);
  Wire.write(0x04); // select channel 0
  //Wire.read();
     //Serial.println(Wire.read());
  Wire.endTransmission();   
  delay(10); 

 
 //Serial.println("switch");


  sensorVersion = as726xSetup();

 // Serial.println(String(sensorVersion));
  
  if (sensorVersion == 0)
  {
    //Serial.println("Sensor failed to respond. Check wiring.");
    while (1); //Freeze!
  }

//  delay(50); 
  takeMeasurements(); //No LED - easier on your eyes

  
  if(sensorVersion == SENSORTYPE_AS7263)
  {
    /*
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
      Serial.print("] tempF[");
  Serial.print(tempF, 1);
  Serial.print("]");

  Serial.println();


    String HIGHS = base64::encode(String(getCalibratedR()*0.01)+":"+String(getCalibratedS()*0.01)+":"+String(getCalibratedT()*0.01)+":"+String(getCalibratedU()*0.01)+":"+String(getCalibratedV()*0.01)+":"+String(getCalibratedW()*0.01) +":H")  ;
  delay(10);
    HIGHS.toCharArray(hsendstr, 45);
*/   

readings[6] = getCalibratedR()*0.01;
readings[7] = getCalibratedS()*0.01;
readings[8] = getCalibratedT()*0.01; 
readings[9] = getCalibratedU()*0.01;
readings[10] = getCalibratedV()*0.01; 
readings[11] = getCalibratedW()*0.01; 
  }
 //softReset();
   //delay(2000); 

   // Serial.println("------------------------------------------------------------------------------------");
}
