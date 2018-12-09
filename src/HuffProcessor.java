import java.util.*;

import javax.sound.sampled.AudioFormat.Encoding;

/**
 * Although this class has a history of several years,
 * it is starting from a blank-slate, new and clean implementation
 * as of Fall 2018.
 * <P>
 * Changes include relying solely on a tree for header information
 * and including debug and bits read/written information
 * 
 * @author Owen Astrachan
 */

public class HuffProcessor {

	public static final int BITS_PER_WORD = 8;
	public static final int BITS_PER_INT = 32;
	public static final int ALPH_SIZE = (1 << BITS_PER_WORD); 
	public static final int PSEUDO_EOF = ALPH_SIZE;
	public static final int HUFF_NUMBER = 0xface8200;
	public static final int HUFF_TREE  = HUFF_NUMBER | 1;

	private final int myDebugLevel;
	
	public static final int DEBUG_HIGH = 4;
	public static final int DEBUG_LOW = 1;
	
	public HuffProcessor() {
		this(0);
	}
	
	public HuffProcessor(int debug) {
		myDebugLevel = debug;
	}

	/**
	 * Compresses a file. Process must be reversible and loss-less.
	 *
	 * @param in
	 *            Buffered bit stream of the file to be compressed.
	 * @param out
	 *            Buffered bit stream writing to the output file.
	 */
	public void compress(BitInputStream in, BitOutputStream out){

//		while (true){
//			int val = in.readBits(BITS_PER_WORD);
//			if (val == -1) break;
//			out.writeBits(BITS_PER_WORD, val);
//		}
//		out.close();
		
		int[] count = readForCounts(in);
		HuffNode root = makeTreeFromCounts(count);
		String[] codings = makeCodingFromTree(root);
		
		out.writeBits(BITS_PER_INT, HUFF_TREE);
		writeHeader(root, out);
		
		in.reset();
		writeCompressBits(codings, in, out);
		//out.writeBits(1, Integer.parseInt("1",2));
		//out.writeBits(9, Integer.parseInt(codings[PSEUDO_EOF],2));
		out.close();
		
	}
	
	private void writeCompressBits(String[] codings, BitInputStream in, BitOutputStream out) {
		while(true) {
		
		int a = in.readBits(BITS_PER_WORD);
		
		if(a == -1) {
			break;
		}
		String Compcode = codings[a];
		out.writeBits(Compcode.length(), Integer.parseInt(Compcode,2));
		
		}
		String lastcode = codings[PSEUDO_EOF];
		out.writeBits(lastcode.length(), Integer.parseInt(lastcode,2));
		return;
	}
	
	private void writeHeader(HuffNode root, BitOutputStream out) {
		HuffNode current = root;
		if(current.myLeft == null && current.myRight == null) {
			if(current.myValue == PSEUDO_EOF) {
				out.writeBits(1, 1);
				out.writeBits(BITS_PER_WORD + 1, current.myValue);
				return;
			}
			out.writeBits(1, 1);
			out.writeBits(BITS_PER_WORD + 1, current.myValue);
			return;
		}
		else {
		out.writeBits(1, 0);
		writeHeader(root.myLeft, out);
		writeHeader(root.myRight, out);
		}
		
//		HuffNode current = root; 
//		   while (true) {
//		       int bits = in.readBits(1);
//		       if (bits == -1) {
//		           throw new HuffException("bad input, no PSEUDO_EOF");
//		       } else { 
//		    	   if (bits == 0) current = current.myLeft;
//		    	   else current = current.myRight;
//		           if (current.myLeft == null && current.myRight == null) {
//		               if (current.myValue == PSEUDO_EOF) 
//		                   break;   // out of loop
//		               else {
//		            	   out.writeBits(BITS_PER_WORD, current.myValue);
//		                   current = root; // start back after leaf
//		               }
//		           }
//		       }
//		   }
		
	}
	
	private String[] makeCodingFromTree(HuffNode root) {
	     String[] encodings = new String[ALPH_SIZE + 1];
	     codingHelper(root,"",encodings);
	     return encodings;
	}
	
	private void codingHelper(HuffNode root, String path, String[] encodings) {
		HuffNode toleaf = root;
		if(toleaf.myLeft == null && toleaf.myRight == null) {
			if(toleaf.myValue == PSEUDO_EOF) {
				encodings[ALPH_SIZE] = path;	
				return;
			}				
			encodings[toleaf.myValue] = path;
			return;
		}	
		for(int i = 0; i < 2; i++){
			HuffNode curr = root;
			if(i == 0){
				curr = curr.myLeft;
				path = path + "0";
				codingHelper(curr, path, encodings);
			}else{
				curr = curr.myRight;
				path = path + "1";
				codingHelper(curr, path, encodings);
			}
			path = path.substring(0, path.length() - 1);
		}
//			codingHelper(toleaf.myLeft, path += "0", encodings);
//			codingHelper(toleaf.myRight, path += "1", encodings);
	}
	
	private HuffNode makeTreeFromCounts(int[] count) {
		PriorityQueue<HuffNode> pq = new PriorityQueue<HuffNode>();
		//loop every index such that freq[index] > 0
		for(int i = 0; i < count.length; i++) {
			if(count[i] > 0) {
		    pq.add(new HuffNode(i, count[i] , null, null));
			}
		}
		//pq.add(new HuffNode(PSEUDO_EOF, 1, null,null));

		while (pq.size() > 1) {
		    HuffNode left = pq.remove();
		    HuffNode right = pq.remove();
		    //here is a difference, which value should be put?
		    HuffNode t = new HuffNode(-1, 
		    		left.myWeight + right.myWeight, left, right);
		    
		    // create new HuffNode t with weight from
		    // left.weight+right.weight and left, right subtrees
		    pq.add(t);
		}
		HuffNode root = pq.remove();
		return root;

	}
	
	private int[] readForCounts(BitInputStream in) {
		
		int[] storage = new int[ALPH_SIZE + 1];
		int read = 0;
		//initialize
		for(int i = 0; i < storage.length; i++) {
			storage[i] = 0;
		}
		while(true) {
			read = in.readBits(BITS_PER_WORD);
		
			if(read == -1) break;
			
			storage[read] = storage[read] + 1;
		}
		
		storage[PSEUDO_EOF] = 1;
		
		return storage;
		}
	
	
	
	
	
	/**
	 * Decompresses a file. Output file must be identical bit-by-bit to the
	 * original.
	 *
	 * @param in
	 *            Buffered bit stream of the file to be decompressed.
	 * @param out
	 *            Buffered bit stream writing to the output file.
	 */
	public void decompress(BitInputStream in, BitOutputStream out){
//		while (true){
//			int val = in.readBits(BITS_PER_WORD);
//			if (val == -1) break;
//			out.writeBits(BITS_PER_WORD, val);
//		}
		
		int bits = in.readBits(BITS_PER_INT);
		if(bits != HUFF_TREE) {
			throw new HuffException("illegal header starts with " + bits);
		}
		
		HuffNode root = readTreeHeader(in);
		readCompressedBits(root, in, out);
		out.close();
	}
	

	private HuffNode readTreeHeader(BitInputStream in) {
		int bits = in.readBits(1);		
		int value;
		if(bits == -1) {
			throw new HuffException("illegal header starts with " + bits);
		}		
		if(bits == 0) {			
			HuffNode Left = readTreeHeader(in);
			HuffNode Right = readTreeHeader(in);
			return new HuffNode(0, 0, Left, Right);
		}
		else {
			value = in.readBits(9);
			return new HuffNode(value,0,null,null);
		}		
	}
	
	private void readCompressedBits(HuffNode root, BitInputStream in, BitOutputStream out) {
		   HuffNode current = root; 
		   while (true) {
		       int bits = in.readBits(1);
		       if (bits == -1) {
		           throw new HuffException("bad input, no PSEUDO_EOF");
		       } else { 
		    	   if (bits == 0) current = current.myLeft;
		    	   else current = current.myRight;
		           if (current.myLeft == null && current.myRight == null) {
		               if (current.myValue == PSEUDO_EOF) 
		                   break;   // out of loop
		               else {
		            	   out.writeBits(BITS_PER_WORD, current.myValue);
		                   current = root; // start back after leaf
		               }
		           }
		       }
		   }		   
	}
	
//    private String[] subtreespliter(String allbits) {
//        String[] temp = allbits.split("");
//        int myCountZero = 0;
//        int myCountOne = 0;
//        int s = 1; //s is the index of the array, 0 is ignored(preorder traversal)
//        int[] lrsubtree = new int[2];
//        String[] tempret = new String[2];
//        while(s < temp.length) {
//            if(temp[s].equals("0")) {
//                myCountZero ++;
//                s++;
//            }
//            if(temp[s].equals("1")){
//                myCountOne ++;
//                s += 10;
//            }
//            if(myCountOne - 1 == myCountZero) {
//                break;
//            }
//        }
//        tempret[0] = allbits.substring(1, s);
//        tempret[1] = allbits.substring(s);
//        return tempret;
//    }
	
	
	
	
}