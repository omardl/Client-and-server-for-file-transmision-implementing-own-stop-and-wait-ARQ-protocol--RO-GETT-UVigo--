import java.io.*;
import java.net.*;
import java.util.Arrays;

//SERVIDOR
public class ro1920recv {
		
	public static BufferedOutputStream buffer;
	
	public static InetAddress IP_shuffle;
	public static int puerto_shuffle;
	
	//En el origen se introducen la IP y puerto del servidor para que el shuffle pueda realizar el intercambio de IPs
	public static byte [] origen = new byte [6];
	
	
	//Función que extrae todos los datos del paquete y los introduce en el fichero
	public static void extraer_datos (DatagramPacket paq) throws IOException {
    
		byte [] data = Arrays.copyOfRange(paq.getData(), 6, paq.getLength() - 12);
		buffer.write(data);
        	return;
	}

	
	//Funcion para la extracción del timestamp del paquete
	public static byte [] extraer_sello (DatagramPacket paq) {
		
		byte [] sello = Arrays.copyOfRange(paq.getData(), paq.getLength() - 12, paq.getLength() - 4);
		return sello
	}

	
	//Función para la creación de los paquetes ACK
	public static DatagramPacket crear_ack (int Cr, byte [] sello) throws IOException {
		
		ByteArrayOutputStream buff = new ByteArrayOutputStream();
		
		buff.write(origen);	
		buff.write(sello);
		buff.write(DeIntAByteArray(Cr));
		
		return new DatagramPacket(buff.toByteArray(), buff.toByteArray().length, IP_shuffle, puerto_shuffle); 
	}
	
	
	//Función para la extracción de los 6 bytes iniciales de los datos con la direccion de origen del paquete
	public static void extraer_origen (DatagramPacket paq) {
		
		origen = Arrays.copyOfRange(paq.getData(), 0, 6);		
		return;
	}
	
	
	//Función para la extración del Ce (Nº de secuencia de los paquetes enviados por el cliente), lo devuelve como entero
	public static int extraer_Ce (DatagramPacket paq) {
		
		return DeByteArrayAInt(Arrays.copyOfRange(paq.getData(), paq.getLength() - 4, paq.getLength()));
	}
	
	
	
	public static void main(String [] args) throws IOException {
			
		//Se comprueba que el número de argumentos de llamada es el  correcto
		if (args.length != 2) {	
			
			System.out.println("La sintaxis correcta de entrada es: ro1920recv output_file listen_port");
			return;
		}

		
		FileOutputStream archivo = new FileOutputStream(args[0]);
		buffer = new BufferedOutputStream(archivo);
		
		int puerto_escucha = Integer.parseInt(args[1]);
		
		
		//Inicialización de variables
		//Cr será el contador de números de secuencia de los ACKS enviados 
		int Cr = 0;
		
		byte buff [] = new byte[1472];
		
		DatagramSocket socketUDP = new DatagramSocket(puerto_escucha);
		
		DatagramPacket paquete = new DatagramPacket(buff, buff.length);
		
		
		//Se espera el primer paquete y se envía su ack antes de entrar en el bucle principal
		socketUDP.receive(paquete);
		
		IP_shuffle = paquete.getAddress();
		
		puerto_shuffle = paquete.getPort();
		
		extraer_origen(paquete);
		
		extraer_datos(paquete);
		
		socketUDP.send(crear_ack(Cr, extraer_sello(paquete)));
		
	    	Cr++;
		
	    	/*Tras enviar el primer paquete, se configura un timeout considerable (5s) para todos los paquetes. 
	    	Si el timeout se agota, se considera que el emisor ha dejado de enviar y termina la recepcion. */
		while (true) {
			
			try {
				//Recepción de un paquete
				paquete = new DatagramPacket(buff, buff.length);
				
				socketUDP.setSoTimeout(5000);
				socketUDP.receive(paquete);                        
				
				//Si el Ce es el esperado se extraen los datos, se envia el ack y se incrementa el Cr
				if (Cr == extraer_Ce(paquete)) {
					
					extraer_datos(paquete);
					socketUDP.send(crear_ack(Cr, extraer_sello(paquete)));
					Cr++;
					
				//Si el Ce no es el esperado se reenvía el ack anterior	
				} else {
					
					socketUDP.send(crear_ack(extraer_Ce(paquete), extraer_sello(paquete)));
				}
				
			//Si el timeout se agota, el programa finaliza
			} catch (SocketTimeoutException e) {

           		        System.out.println("Archivo recibido");
                
               			buffer.close();
				archivo.close();
				socketUDP.close();	
				return;
			}	
		}	
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
