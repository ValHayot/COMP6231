package FlightReservationApp;


/**
* FlightReservationApp/FlightReservationOperations.java .
* Generated by the IDL-to-Java compiler (portable), version "3.2"
* from FlightReservation.idl
* Sunday, November 6, 2016 11:45:09 PM EST
*/

public interface FlightReservationOperations 
{
  String bookFlight (String firstName, String lastName, String address, String phone, String destination, String date, String flightClass);
  String getBookedFlightCount (String recordType);
  String editFlightRecord (String recordID, String fieldName, String newValue);
  String transferReservation (String passengerID, String currentCity, String otherCity);
} // interface FlightReservationOperations
