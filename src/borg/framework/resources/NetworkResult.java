package borg.framework.resources;

public enum NetworkResult
{
	/** 0: unknown result **/
	_UNKNOWN,

	/** 1: operation succeeded **/
	SUCCESS,

	/** 2: general failure **/
	FAILURE,

	/** 3: busy **/
	BUSY,

	/** 4: no network connection **/
	NOT_CONNECTED,

	/** 5: unable to connect to the host **/
	UNABLE_TO_CONNECT,

	/** 6: unable to send data **/
	UNABLE_TO_SEND,

	/** 7: unable to read data **/
	UNABLE_TO_READ,

	/** 8: unexpected server response **/
	UNEXPECTED_RESPONSE,

	/** 9: disconnect occurred **/
	DISCONNECT
}