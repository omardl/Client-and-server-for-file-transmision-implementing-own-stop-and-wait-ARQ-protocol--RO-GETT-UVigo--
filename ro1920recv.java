import java.io.*;
import java.net.*;
import java.util.Arrays;


public class ro1920recv {
	
	
	public static BufferedOutputStream buffer;
	
	public static InetAddress IP_shuffle;
	
	public static int puerto_shuffle;
	
	public static byte [] origen = new byte [6];
	
	
	//Funcion para extraer los datos de cada paquete y agregarlos al fichero
	public static void extraer_datos (DatagramPacket paq) throws IOException {
    
		byte [] data = Arrays.copyOfRange(paq.getData(), 6, paq.getLength() - 12);

		buffer.write(data);

        return;
		
	}

	
	//Funcion que extrae el byte [] que contiene el sello temporal del paquete
	public static byte [] extraer_sello (DatagramPacket paq) {
		
		byte [] sello = Arrays.copyOfRange(paq.getData(), paq.getLength() - 12, paq.getLength() - 4);
		
		return sello;
		
	}

	
	//Funcion que crea el paquete ack a partir del Cr
	public static DatagramPacket crear_ack (int Cr, byte [] sello) throws IOException {
		
		ByteArrayOutputStream buff = new ByteArrayOutputStream();
		
		buff.write(origen);
		
		buff.write(sello);
		
		buff.write(DeIntAByteArray(Cr));
				
		return new DatagramPacket(buff.toByteArray(), buff.toByteArray().length, IP_shuffle, puerto_shuffle);
		 
	}
	
	
	//Funcion que extrae los 6 bytes iniciales con la direccion de origen del paquete
	public static void extraer_origen (DatagramPacket paq) {
		
		origen = Arrays.copyOfRange(paq.getData(), 0, 6);
		
		return;
		
	}
	
	
	//Funcion que extrae el Ce del paquete recibido y lo devuelve como entero
	public static int extraer_Ce (DatagramPacket paq) {
		
		return DeByteArrayAInt(Arrays.copyOfRange(paq.getData(), paq.getLength() - 4, paq.getLength()));
		
	}
	
	
	//FUNCION PRINCIPAL
	public static void main(String [] args) throws IOException {
		
		
		//Comprobamos que la cantidad de argumentos recibidos es la correcta
		if (args.length != 2) {
			
			System.out.println("La sintaxis correcta de entrada es: ro1920recv output_file listen_port");
			
			return;
			
		}

		
		//Recogemos los argumentos recibidos por parametros
		FileOutputStream archivo = new FileOutputStream(args[0]);

		buffer = new BufferedOutputStream(archivo);
			
		int puerto_escucha = Integer.parseInt(args[1]);
		
		
		//Inicializamos variables
		int Cr = 0;
		
		byte buff [] = new byte[1472];
		
		DatagramSocket socketUDP = new DatagramSocket(puerto_escucha);
		
		DatagramPacket paquete = new DatagramPacket(buff, buff.length);
		
		
		//Esperamos el primer paquete y enviamos su ack antes de entrar en el bucle principal
		socketUDP.receive(paquete);
		
		IP_shuffle = paquete.getAddress();
		
		puerto_shuffle = paquete.getPort();
		
		extraer_origen(paquete);
		
		extraer_datos(paquete);
		
		socketUDP.send(crear_ack(Cr, extraer_sello(paquete)));
		
	    Cr++;
		
	    
	    /*Tras enviar el primer paquete, se configura un timeout considerable (5s) para todos los paquetes. 
	    Si el timeout se agota, consideramos que el emisor a dejado de enviar y termina la recepcion. */
		while (true) {
			
			try {
			
				//Recibimos un paquete
				paquete = new DatagramPacket(buff, buff.length);
				
				socketUDP.setSoTimeout(5000);
				
				socketUDP.receive(paquete);                        
				
				//Si el Ce es el esperado extraemos los datos, enviamos el ack e incrementamos contador
				if (Cr == extraer_Ce(paquete)) {
					
					extraer_datos(paquete);
					
					socketUDP.send(crear_ack(Cr, extraer_sello(paquete)));
					
					Cr++;
					
				//Si el Ce no es el esperado reenviamos el ack anterior	
				} else {
					
					socketUDP.send(crear_ack(extraer_Ce(paquete), extraer_sello(paquete)));
					
				}
				
			//Si el timeout se agota, cerramos todo y salimos del programa
			} catch (SocketTimeoutException e) {

                System.out.println("Archivo recibido");
                
                buffer.close();
 
				archivo.close();
				
				socketUDP.close();
				
				return;
				
			}
			
		}
		
	}
	

	//Funciones que devuelven un byte [] a partir de un int y viceversa, usados para los numeros de secuencia
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

