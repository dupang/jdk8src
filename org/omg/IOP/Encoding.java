package org.omg.IOP;


/**
* org/omg/IOP/Encoding.java .
* Generated by the IDL-to-Java compiler (portable), version "3.2"
* from c:/re/workspace/8-2-build-windows-amd64-cygwin/jdk8u74/6087/corba/src/share/classes/org/omg/PortableInterceptor/IOP.idl
* Friday, January 29, 2016 5:44:11 PM PST
*/

public final class Encoding implements org.omg.CORBA.portable.IDLEntity
{

  /**
     * The encoding format.
     */
  public short format = (short)0;

  /**
     * The major version of this Encoding format.
     */
  public byte major_version = (byte)0;

  /**
     * The minor version of this Encoding format.
     */
  public byte minor_version = (byte)0;

  public Encoding ()
  {
  } // ctor

  public Encoding (short _format, byte _major_version, byte _minor_version)
  {
    format = _format;
    major_version = _major_version;
    minor_version = _minor_version;
  } // ctor

} // class Encoding
