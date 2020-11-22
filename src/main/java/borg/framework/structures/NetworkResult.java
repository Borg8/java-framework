package borg.framework.structures;

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

	/** 4: operation timeout **/
	TIMEOUT,

	/** 5: not connected to the host **/
	NOT_CONNECTED,

	/** 6: unable to connect to the host **/
	UNABLE_TO_CONNECT,

	/** 7: unable to send data **/
	UNABLE_TO_SEND,

	/** 8: unable to read data **/
	UNABLE_TO_READ,

	/** 9: unexpected server response **/
	UNEXPECTED_RESPONSE,

	/** 10: disconnect occurred **/
	DISCONNECT
}