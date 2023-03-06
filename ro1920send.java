import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.Arrays;

//CLIENTE
public class ro1920send {
	
	public static boolean fin = false;
	public static boolean primera_medida_RTO = true;
	
	public static long RTO, media_rtt_anterior, desviacion_anterior;
	
	public static InetAddress ip_servidor, ip_shuffle;
        public static short puerto_servidor;	
	public static int puerto_shuffle;
	
	public static int bytes_de_datos_enviados = 0;
	public static byte [] datos;
	
	
	//Funcion que construye cada paquete y lo devuelve, a partir de su Ce (Nº de secuencia)
	public static DatagramPacket construye_paquete (int Ce) throws IOException {
		
		ByteArrayOutputStream buffer = new ByteArrayOutputStream();
		
		//Los primeros 6 bytes deben ser IP y puerto destino
		buffer.write(ip_servidor.getAddress());
		buffer.write(DeShortAByteArray(puerto_servidor));
        
		//Los siguientes 1454 bytes son de datos (MTU de 1500 bytes, 40 para la cabecera IP)
		byte [] data;

        	//Cuando se llega al final del archivo y es el ultimo paquete, se sale del bucle
                if ((bytes_de_datos_enviados + 1454) < datos.length) {

                	data = Arrays.copyOfRange(datos, bytes_de_datos_enviados, bytes_de_datos_enviados + 1454);
                } else {

                	data = Arrays.copyOfRange(datos, bytes_de_datos_enviados, datos.length); 
                        fin = true;
                }

		buffer.write(data);

		//Los siguientes 8 bytes son del timestamp
		buffer.write(DeLongAByteArray(System.currentTimeMillis()));
		
		//Los ultimos 4 bytes son del Ce
		buffer.write(DeIntAByteArray(Ce));
		
		DatagramPacket paquete = new DatagramPacket(buffer.toByteArray(), buffer.toByteArray().length, ip_shuffle, puerto_shuffle);
                
                buffer.close();	
		return paquete;
	}
	
	
	////Funcion para la extracción del Cr del paquete, lo devuelve como entero
	public static int extrae_Cr (byte [] ack_recibido) {
		
		byte [] Cr = Arrays.copyOfRange(ack_recibido, ack_recibido.length - 4, ack_recibido.length);
		return DeByteArrayAInt(Cr);
	}
	
	
	//Funcion para el ajuste del RTO (temporizador de retransmisión)
	public static void ajuste_RTO(byte [] paq) {
		
            long rtt, media_rtt, desviacion_rtt;	
            rtt = System.currentTimeMillis() - DeByteArrayALong(Arrays.copyOfRange(paq, paq.length - 12, paq.length -4));

	    //Si es la primera medida, el calculo estimado es diferente
        	if(primera_medida_RTO == true) {
			
			primera_medida_RTO = false;
			media_rtt = rtt;
			desviacion_rtt = rtt/2;
			
		} else {
			
			media_rtt = (1 - (1/8))*media_rtt_anterior + (1/8)*rtt;
			desviacion_rtt = (1 - (1/4))*desviacion_anterior + (1/4)*Math.abs(media_rtt - rtt);
		}
		
		media_rtt_anterior = media_rtt;
		desviacion_anterior = desviacion_rtt;
		
		RTO = media_rtt + 4*desviacion_rtt;
		
		return;
	}
	

	//FUNCION PRINCIPAL
	public static void main(String [] args) throws IOException {
	
		//Se comprueba que el número de argumentos de llamada es el correcto
		if (args.length != 5) {
			
			System.out.println("La sintaxis correcta de entrada es: ro1920send input_file dest_IP dest_port emulator_IP emulator_port");
			return;
		}
	
		FileInputStream archivo = new FileInputStream(args[0]);		
		datos = archivo.readAllBytes();
		
		ip_servidor = InetAddress.getByName(args[1]);
		ip_shuffle = InetAddress.getByName(args[3]);
		
		puerto_servidor = Short.parseShort(args[2]);		
		puerto_shuffle = Integer.parseInt(args[4]);
		
	 
		//Inicialización de variables		
		int Ce = 0;
		RTO = 100;
		
		DatagramSocket socketUDP = new DatagramSocket();
		DatagramPacket envio;
		
		byte [] ack = new byte [18];
		
		DatagramPacket recepcion = new DatagramPacket(ack, ack.length);
		
		//Comienzo del envio
		while (fin == false) {
			
			//Construcción y envío de un paquete
			envio = construye_paquete(Ce);
			socketUDP.send(envio);
			
			try {
				//Recepción de un ACK
				socketUDP.setSoTimeout((int)RTO);				
				socketUDP.receive(recepcion);
	
				//Si el ACK es el esperado: aumentamos Ce y el "puntero" de los bytes enviados y ajustamos el RTO
				if (Ce == extrae_Cr(recepcion.getData())) {		

					Ce++;
                    			bytes_de_datos_enviados = bytes_de_datos_enviados + 1454;

					ajuste_RTO(recepcion.getData());
				}
				
			} catch (SocketTimeoutException e2) {

				if (fin == true) {
					
					//Si ya se ha enviado el ultimo paquete pero salta el timeout no sale del while
					fin = false;
				}				
			}
		}
                
		System.out.println("Envio finalizado");
		
		archivo.close();
		socketUDP.close();
		return;
	}
	
	
	//Función para la transformación de short a byte[] 
	public static byte [] DeShortAByteArray(short data) {

	    byte [] bytes = new byte[2];
	    ByteBuffer buff = ByteBuffer.allocate(bytes.length);
	    buff.putShort(data);
	    return buff.array();
	}
	
	
	//Función para la transformación de long a byte[] 
	public static byte[] DeLongAByteArray (long num) {
		
		ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
		buffer.putLong(num);
		return buffer.array();
	}
	

	//Función para la transformación de byte[] a long 
	public static long DeByteArrayALong(byte[] bytes) {
		
	    ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
	    buffer.put(bytes);
	    buffer.flip();
	    return buffer.getLong();
	}
	
	
	//Función para la transformación de int a byte[]
	public static byte[] DeIntAByteArray(int data) {
		
		return new byte[] {
				(byte)((data >> 24) & 0xff),
				(byte)((data >> 16) & 0xff),
				(byte)((data >> 8) & 0xff),
				(byte)((data >> 0) & 0xff),
		};
	}


	//Función para la transformación de byte[] a int
	public static int DeByteArrayAInt(byte[] bytes) {
	
		return (int)(
			(0xff & bytes[0]) << 24 |
			(0xff & bytes[1]) << 16 |
			(0xff & bytes[2]) << 8  |
			(0xff & bytes[3]) << 0
			);
	}
}
