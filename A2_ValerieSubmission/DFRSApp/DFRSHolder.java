package DFRSApp;

/**
* DFRSApp/DFRSHolder.java .
* Generated by the IDL-to-Java compiler (portable), version "3.2"
* from /Users/valeriehayot/Documents/DistributedSystems/4.Project/A2-fix/COMP6321_A2/Server/src/DFRS.idl
* Sunday, November 13, 2016 3:23:52 PM EST
*/

public final class DFRSHolder implements org.omg.CORBA.portable.Streamable
{
  public DFRSApp.DFRS value = null;

  public DFRSHolder ()
  {
  }

  public DFRSHolder (DFRSApp.DFRS initialValue)
  {
    value = initialValue;
  }

  public void _read (org.omg.CORBA.portable.InputStream i)
  {
    value = DFRSApp.DFRSHelper.read (i);
  }

  public void _write (org.omg.CORBA.portable.OutputStream o)
  {
    DFRSApp.DFRSHelper.write (o, value);
  }

  public org.omg.CORBA.TypeCode _type ()
  {
    return DFRSApp.DFRSHelper.type ();
  }

}
