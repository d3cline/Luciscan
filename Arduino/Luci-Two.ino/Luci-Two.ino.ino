#include <Wire.h>
byte sensorVersion = 0; //Keeps track of whether we have an AS7262 or AS7263
#define SENSORTYPE_AS7262 0x3E
#define SENSORTYPE_AS7263 0x3F

#include <esp_log.h>
#include <string>
#include <sys/time.h>
#include <sstream>

#include "sdkconfig.h"   
    
#include <base64.h>

#include <BLEDevice.h>
#include <BLEUtils.h>
#include <BLEServer.h>


// See the following for generating UUIDs:
// https://www.uuidgenerator.net/

#define LOW_CHAR_UUID        "4fafc201-1fb5-459e-8fcc-000000000001"
//#define HIGH_CHAR_UUID        "4fafc201-1fb5-459e-8fcc-000000000002"

#define INT_UUID "4fafc201-1fb5-459e-8fcc-000000494e54"


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
float readings[6];

int snum = 0;

class MyCallbackHandler: public BLECharacteristicCallbacks {
  
  void onRead(BLECharacteristic *pCharacteristic) {
    String SEND = String(snum)+":"+String(readings[snum]);
    SEND.toCharArray(vsendstr, 23);
    pCharacteristic->setValue(vsendstr);
    ++snum;
    if(snum == 6){
      //Serial.print("6");
      snum = 0;
    }
  
  }
};


extern int INTEGRATION;

class IntCallbacks: public BLECharacteristicCallbacks {
    void onWrite(BLECharacteristic *intCharacteristic) {
      std::string value = intCharacteristic->getValue();
      if (value.length() > 0) {
        String VAL;
        for (int i = 0; i < value.length(); i++){
        VAL = VAL +value[i];
        }
        int VALINT = VAL.toInt();
        if(VALINT > 0 and VALINT < 256){
        INTEGRATION = VALINT;
        }
      }
    }

void onRead(BLECharacteristic *pCharacteristic) {
    String SEND = String(INTEGRATION);
    SEND.toCharArray(vsendstr, 23);
    pCharacteristic->setValue(vsendstr);  
  }
    
};




void setup()
{
pinMode(12, OUTPUT);  
Wire.begin();
//Serial.begin(115200);
//  Serial.print("Booting");

 
BLEDevice::init("Luci");
//BLEDevice::setMTU(80);
BLEDevice::setPower(ESP_PWR_LVL_N5);

BLEServer *pServer = BLEDevice::createServer();
BLEService *pService = pServer->createService(BLEUUID(SERVICE_UUID_BIN, 16, true));

BLECharacteristic *pCharacteristic = pService->createCharacteristic(
BLEUUID(LOW_CHAR_UUID),
BLECharacteristic::PROPERTY_READ
);

  pCharacteristic->setCallbacks(new MyCallbackHandler());
  pCharacteristic->setValue("Hello World");



/*
  BLECharacteristic *intReadCharacteristic = pService->createCharacteristic(
    BLEUUID(INT_READ_UUID),
    BLECharacteristic::PROPERTY_READ
  );
  intReadCharacteristic->setCallbacks(new IntReadCallbackHandler());
  intReadCharacteristic->setValue("28");

*/


  BLECharacteristic *intCharacteristic = pService->createCharacteristic(
                                         INT_UUID,
                                         BLECharacteristic::PROPERTY_READ |
                                         BLECharacteristic::PROPERTY_WRITE
                                       );

  intCharacteristic->setCallbacks(new IntCallbacks());

  intCharacteristic->setValue("Hello World");




  pService->start();
  BLEAdvertising *pAdvertising = pServer->getAdvertising();
  pAdvertising->addServiceUUID(BLEUUID("4fafc201-1fb5-459e-8fcc-0000000000ff"));
  pAdvertising->start();
 
 // Serial.print("init finished");
  digitalWrite(12, LOW);    // turn the LED off by making the voltage LOW
  delay(5); 




readSensors();

}

void loop()
{
 
      if(snum == 5){
        readSensors();
    }
  
}

void readSensors() {

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
/*
Serial.println(readings[0]); 
Serial.println(readings[1]); 
Serial.println(readings[2]); 
Serial.println(readings[3]); 
Serial.println(readings[4]); 
Serial.println(readings[5]); 
*/
  
  }


   // Serial.println("-------------------------------------------------------------- ----------------------");
}
