import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.Arrays;


public class ro1920send {
	
	public static boolean fin = false;
	
	public static boolean primera_medida_RTO = true;
	
	public static int bytes_de_datos_enviados = 0;
	
	public static long RTO, media_rtt_anterior, desviacion_anterior;
	
	public static InetAddress ip_servidor, ip_shuffle;

        public static short puerto_servidor;	

	public static int puerto_shuffle;
	
	public static byte [] datos;
	
	
	//Funcion que construye cada paquete y lo devuelve, a partir de su Ce
	public static DatagramPacket construye_paquete (int Ce) throws IOException {
		
		ByteArrayOutputStream buffer = new ByteArrayOutputStream();
		
		//Los primeros 6 bytes son IP y puerto destino
		buffer.write(ip_servidor.getAddress());
		
		buffer.write(DeShortAByteArray(puerto_servidor));

        
		//Los siguientes 1454 bytes son de datos
		byte [] data;

        	//Cuando se llega al final del archivo y es el ultimo paquete, se cambia la variable fin para salir del while del main
                if ((bytes_de_datos_enviados + 1454) < datos.length) {

                	data = Arrays.copyOfRange(datos, bytes_de_datos_enviados, bytes_de_datos_enviados + 1454);

                } else {

                	data = Arrays.copyOfRange(datos, bytes_de_datos_enviados, datos.length); 

                        fin = true;
    
                }

		buffer.write(data);

		
		//Los siguientes 8 bytes son del sello temporal
		buffer.write(DeLongAByteArray(System.currentTimeMillis()));
		
		//Los ultimos 4 bytes son del Ce
		buffer.write(DeIntAByteArray(Ce));
		
		DatagramPacket paquete = new DatagramPacket(buffer.toByteArray(), buffer.toByteArray().length, ip_shuffle, puerto_shuffle);
                
                buffer.close();		

		return paquete;
		
	}
	
	
	//Funcion que extrae el Cr del ack y lo devuelve como entero
	public static int extrae_Cr (byte [] ack_recibido) {
		
		byte [] Cr = Arrays.copyOfRange(ack_recibido, ack_recibido.length - 4, ack_recibido.length);
		
		return DeByteArrayAInt(Cr);
		
	}
	
	
	//Funcion para ajustar las nuevas medidas del RTO
	public static void ajuste_RTO(byte [] paq) {
		
            long rtt, media_rtt, desviacion_rtt;
		
            rtt = System.currentTimeMillis() - DeByteArrayALong(Arrays.copyOfRange(paq, paq.length - 12, paq.length -4));

	    //Si es la primera medida, el calculo es diferente
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

		
		//Comprobamos que el numero de parametros es el correcto
		if (args.length != 5) {
			
			System.out.println("La sintaxis correcta de entrada es: ro1920send input_file dest_IP dest_port emulator_IP emulator_port");
			
			return;
		
		}
	
		
		//Recogemos los parametros de entrada
		FileInputStream archivo = new FileInputStream(args[0]);
		
		datos = archivo.readAllBytes();
		
		ip_servidor = InetAddress.getByName(args[1]);
		
		ip_shuffle = InetAddress.getByName(args[3]);
		
		puerto_servidor = Short.parseShort(args[2]);		
		
		puerto_shuffle = Integer.parseInt(args[4]);
		
	 
		//Inicializamos variables		
		int Ce = 0;
		
		RTO = 100;
		
		DatagramSocket socketUDP = new DatagramSocket();
		
		DatagramPacket envio;
		
		byte [] ack = new byte [18];
		
		DatagramPacket recepcion = new DatagramPacket(ack, ack.length);
		
		
		//Empezamos el envio
		while (fin == false) {
			
			//Construimos un nuevo paquete y lo enviamos
			envio = construye_paquete(Ce);

			socketUDP.send(envio);
			
			try {
			
				//Recibimos un paquete
				socketUDP.setSoTimeout((int)RTO);
				
				socketUDP.receive(recepcion);
	
				//Si el ack recibido es el esperado: aumentamos Ce y el "puntero" de los bytes enviados y ajustamos el RTO
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
	
	
	//Funcion para convertir un short en un byte [], usado para el puerto del shufflerouter
	public static byte [] DeShortAByteArray(short data) {

	    byte [] bytes = new byte[2];

	    ByteBuffer buff = ByteBuffer.allocate(bytes.length);

	    buff.putShort(data);
	    
	    return buff.array();

	}
	
	
	//Funciones para convertir un long en byte [] y viceversa, usadas para los timestamps
	public static byte[] DeLongAByteArray (long num) {
		
		ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
	    
		buffer.putLong(num);
	    
		return buffer.array();
	
	}
	
	
	public static long DeByteArrayALong(byte[] bytes) {
		
	    ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
	    
	    buffer.put(bytes);
	    
	    buffer.flip();
	
	    return buffer.getLong();
	    
	}
	
	
	//Funciones para convertir un int en byte [] y viceversa, usadas para los numeros de secuencia
	public static byte[] DeIntAByteArray(int data) {
		
		return new byte[] {
		
				(byte)((data >> 24) & 0xff),
		
				(byte)((data >> 16) & 0xff),
		
				(byte)((data >> 8) & 0xff),
		
				(byte)((data >> 0) & 0xff),
	
		};
		
	}


	public static int DeByteArrayAInt(byte[] bytes) {
	
		return (int)(
	
			(0xff & bytes[0]) << 24 |

			(0xff & bytes[1]) << 16 |

			(0xff & bytes[2]) << 8  |

			(0xff & bytes[3]) << 0

			);

	}
	
}
