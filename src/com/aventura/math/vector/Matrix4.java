package com.aventura.math.vector;

import java.util.Arrays;

import com.aventura.math.Constants;
import com.aventura.tools.tracing.Tracer;

/**
 * ------------------------------------------------------------------------------ 
 * MIT License
 * 
 * Copyright (c) 2018 Olivier BARRY
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 * ------------------------------------------------------------------------------ 
**/
public class Matrix4 {

    protected static final float[][] IDENTITY_ARRAY =
    		{{1.0f, 0.0f, 0.0f, 0.0f},
    		 {0.0f, 1.0f ,0.0f, 0.0f},
    		 {0.0f, 0.0f, 1.0f, 0.0f},
    		 {0.0f, 0.0f, 0.0f, 1.0f}};

    public static final Matrix4 IDENTITY = new Matrix4(IDENTITY_ARRAY);

	protected float[][] array;

	/**
	 * Initialize a square Matrix of size s with 0 for all elements of the matrix
	 * @param s the size of the Matrix (number of rows and columns)
	 */
	public Matrix4() {
		// Only create the array
		array = new float[Constants.SIZE_4][Constants.SIZE_4];
		//initialize(0); // optimize -> no init or use Matrix4(0) instead
	}

		
	/**
	 * Initialize Matrix with a constant value for all elements of the matrix
	 * @param val the initialization value
	 */
	public Matrix4(float val) {
		initialize(val);
	}

	/**
	 * Initialize Matrix with a 2D array of double
	 * @param a the 2D array of double
	 */
//	public Matrix4(double[][] a) throws MatrixArrayWrongSizeException {
//		if (a.length != Constants.SIZE_4) throw new MatrixArrayWrongSizeException("Wrong array row size ("+a.length+") while creating Matrix4 from array"); 
//		if (a[0].length != Constants.SIZE_4) throw new MatrixArrayWrongSizeException("Wrong array column size ("+a[0].length+") while creating Matrix4 from array"); 
//
//		this.array = a;
//	}
	public Matrix4(float[][] a) {
		this.array = a;
	}

	/**
	 * Initialize Matrix with data from another Matrix
	 * @param a the other Matrix
	 */
	public Matrix4(Matrix4 a) {
		set(a);
	}
		
	/**
	 * Initialize a Matrix with a constant value for all elements of the matrix
	 * @param val the initialization value
	 */
	protected void initialize(float val) {
		// Create the array
		array = new float[Constants.SIZE_4][Constants.SIZE_4];
		// Initialize values
		for (int i=0; i<Constants.SIZE_4; i++) {
			for (int j=0; j<Constants.SIZE_4; j++) {
				array[i][j] = val;
			}
		}
	}
	
	public void setArray(float[][] a) throws MatrixArrayWrongSizeException {
		if (a.length != Constants.SIZE_4) throw new MatrixArrayWrongSizeException("Wrong array row size ("+a.length+") while creating Matrix4 from array"); 
		if (a[0].length != Constants.SIZE_4) throw new MatrixArrayWrongSizeException("Wrong array column size ("+a[0].length+") while creating Matrix4 from array"); 
		this.array = a;
	}
	
	@Override
	public String toString() {
		String s = "[";
		for (int i=0; i<Constants.SIZE_4-1; i++) {
			s = s + Arrays.toString(array[i]);
			s = s+"\n";
		}
		s = s + Arrays.toString(array[Constants.SIZE_4-1])+"]";
		return s;
	}
	
	/**
	 * Set value of an element of the Matrix
	 * @param i row indice
	 * @param j column indice
	 * @param val value to set
	 */
	public void set(int i, int j, float val) throws IndiceOutOfBoundException {
		if (i<0 || i>Constants.SIZE_4) throw new IndiceOutOfBoundException("Row indice ("+i+") out of bound while setting element ("+i+","+j+") of Matrix4"); 
		if (j<0 || j>Constants.SIZE_4) throw new IndiceOutOfBoundException("Column indice ("+j+") out of bound while setting element ("+i+","+j+") of Matrix4"); 
		array[i][j] = val;
	}

	/**
	 * Set Matrix from another Matrix4
	 * @param a the other Matrix
	 */
	public void set(Matrix4 a) {
		// Create the array
		array = new float[Constants.SIZE_4][Constants.SIZE_4];
		
		for (int i=0; i<Constants.SIZE_4; i++) {
			for (int j=0; j<Constants.SIZE_4; j++) {
				this.array[i][j] = a.array[i][j];
			}
		}
	}


	/**
	 * Get value of an element of the Matrix
	 * @param i row indice
	 * @param j column indice
	 * @return the value of the element
	 */
	public float get(int i, int j) {
		return array[i][j];
	}
	
	public float[][] getArray() {
		return array;
	}
	
	/**
	 * Set all elements of the diagonal of this Matrix to a given value
	 * @param v the value to set
	 */
	public void setDiagonal(float v) {
		for (int i=0; i<Constants.SIZE_4-1; i++) {
			array[i][i] = v;
		}
	}
	
	/**
	 * Get row of a Matrix4 in the format of a Vector4
	 * @param r the rank of the row
	 * @return a Vector4 representing the row
	 * @throws IndiceOutOfBoundException
	 */
	public Vector4 getRow(int r) throws IndiceOutOfBoundException {
		if (r<0 || r>Constants.SIZE_4) throw new IndiceOutOfBoundException("Indice out of bound while getting Row ("+r+") of Matrix4"); 
		float[] array = new float[Constants.SIZE_4];
		Vector4 v = null;
		// No loop for optimization
		array[0] = this.array[r][0];
		array[1] = this.array[r][1];
		array[2] = this.array[r][2];
		array[3] = this.array[r][3];
		
		try {
			v = new Vector4(array);
		} catch (VectorArrayWrongSizeException e) {
			// Do nothing, this won't happen as all arrays are controlled in size (coming from Vector4 and Matrix4)
			if (Tracer.error) Tracer.traceError(this.getClass(), "Unexpected exception: "+e);
			e.printStackTrace();
		}
		 return v;
	}
	
	/**
	 * Set row of a Matrix4 in the format of a Vector4
	 * @param r the rank of the row
	 * @param v a Vector4 representing the row
	 */
	public void setRow(int r, Vector4 v) throws IndiceOutOfBoundException {
		// No loop for optimization
		this.array[r][0] = v.get(0);
		this.array[r][1] = v.get(1);
		this.array[r][2] = v.get(2);
		this.array[r][3] = v.get(3);
	}
	
	/**
	 * Get column of a Matrix4 in the format of a Vector4
	 * @param c the rank of the column
	 * @return a Vector4 representing the column
	 * @throws IndiceOutOfBoundException
	 */
	public Vector4 getColumn(int c) throws IndiceOutOfBoundException {
		if (c<0 || c>Constants.SIZE_4) throw new IndiceOutOfBoundException("Indice out of bound while getting Column ("+c+") of Matrix4"); 
		float[] array = new float[Constants.SIZE_4];
		Vector4 v = null;
		// No loop for optimization
		array[0] = this.array[0][c];
		array[1] = this.array[1][c];
		array[2] = this.array[2][c];
		array[3] = this.array[3][c];
		
		try {
			v = new Vector4(array);
		} catch (VectorArrayWrongSizeException e) {
			// Do nothing, this won't happen as all arrays are controlled in size (coming from Vector4 and Matrix4)
			if (Tracer.error) Tracer.traceError(this.getClass(), "Unexpected exception: "+e);
			e.printStackTrace();
		}
		return v;	
	}
	
	/**
	 * Set column of a Matrix4 in the format of a Vector4
	 * @param c the rank of the column
	 * @param v a Vector4 representing the column
	 * @throws IndiceOutOfBoundException
	 */
	public void setColumn(int c, Vector4 v) throws IndiceOutOfBoundException {
		// No loop for optimization
		this.array[0][c] = v.get(0);
		this.array[1][c] = v.get(1);
		this.array[2][c] = v.get(2);
		this.array[3][c] = v.get(3);
	}
	
	/**
	 * Compare this Matrix with another
	 * @param B the other Matrix
	 * @return true if all the elements of this Matrix are equals to the elements of B
	 */
	public boolean equals(Matrix4 B) {
		
		for (int i=0; i<Constants.SIZE_4; i++) {
			for (int j=0; j<Constants.SIZE_4; j++) {
				if (this.get(i,j) != B.get(i,j)) return false;
			}
		}
		return true;
		
	}
	
	/**
	 * Matrix cross product: C=A^B
	 * @param b the Matrix B
	 * @return C the cross product of this Matrix A with Matrix B provided in parameter 
	 */
	public Matrix4 times(Matrix4 b) {
		Matrix4 r = new Matrix4();
		
		for (int i=0; i<Constants.SIZE_4; i++) {
			for (int j=0; j<Constants.SIZE_4; j++) {
				try {
					r.set(i,j, this.get(i,0)*b.get(0,j) + this.get(i,1)*b.get(1,j) + this.get(i,2)*b.get(2,j) + this.get(i,3)*b.get(3, j));
				} catch (IndiceOutOfBoundException e) {
					// Do nothing, this won't happen as all arrays are controlled in size (coming from Vector3 and Matrix3)
					if (Tracer.error) Tracer.traceError(this.getClass(), "Unexpected exception: "+e);
					e.printStackTrace();				
				}
			}
		}
		return r;
	}
	
	/**
	 * A=A^B
	 * @param B
	 */
	public void timesEquals(Matrix4 b) {
		float[][] array = new float[Constants.SIZE_4][Constants.SIZE_4];
		for (int i=0; i<Constants.SIZE_4; i++) {
			for (int j=0; j<Constants.SIZE_4; j++) {
				array[i][j] = this.get(i,0)*b.get(0,j) + this.get(i,1)*b.get(1,j) + this.get(i,2)*b.get(2,j) + this.get(i,3)*b.get(3, j);
			}
		}
		this.array = array;
	}
	
	/**
	 * Matrix transposition
	 * @return a new Matrix corresponding to the transposition of the current Matrix 
	 */
	public Matrix4 transpose() {
		Matrix4 r = new Matrix4();
		
		for (int i=0; i<Constants.SIZE_4; i++) {
			for (int j=0; j<Constants.SIZE_4; j++) {
				try {
					r.set(i,j, this.get(j, i));
				} catch (IndiceOutOfBoundException e) {
					// Do nothing, this won't happen as all arrays are controlled in size (coming from Vector3 and Matrix3)
					if (Tracer.error) Tracer.traceError(this.getClass(), "Unexpected exception: "+e);
					e.printStackTrace();				
				}
			}
		}
		return r;
	}

	/**
	 * Matrix transposition
	 *Transpose the current Matrix 
	 */
	public void transposeEquals() {
		float[][] array = new float[Constants.SIZE_4][Constants.SIZE_4];
		
		for (int i=0; i<Constants.SIZE_4; i++) {
			for (int j=0; j<Constants.SIZE_4; j++) {
				array[i][j] = this.get(j,i);
			}
		}
		this.array = array;
	}

	/**
	 * Multiply a Matrix by a scalar B=A*s
	 * @param s the scalar value
	 * @return a new Matrix B=A*s
	 */
	public Matrix4 times(float s) {
		Matrix4 r = new Matrix4();
		for (int i=0; i<Constants.SIZE_4; i++) {
			for (int j=0; j<Constants.SIZE_4; j++) {
				try {
					r.set(i,j, this.array[i][j]*s);
				} catch (IndiceOutOfBoundException e) {
					// Do nothing, this won't happen as all arrays are controlled in size (coming from Vector4 and Matrix4)
					if (Tracer.error) Tracer.traceError(this.getClass(), "Unexpected exception: "+e);
					e.printStackTrace();				
				}
			}
		}
		return r;
	}
	
	/**
	 * Multiply this Matrix by a scalar A = A*s
	 * @param s the scalar value
	 */
	public void timesEquals(float s) {
		for (int i=0; i<Constants.SIZE_4; i++) {
			for (int j=0; j<Constants.SIZE_4; j++) {
				this.array[i][j] = this.array[i][j]*s;
			}
		}
	}
	
	/**
	 * Matrix addition C=A+B. Do not modify this Matrix (A), this Matrix and return C a newly created Matrix.
	 * @param B the Matrix to be added
	 * @return C, a new Matrix, sum of this Matrix (A) and B Matrix
	 */
	public Matrix4 plus(Matrix4 B) {
		Matrix4 r = new Matrix4();
		for (int i=0; i<Constants.SIZE_4; i++) {
			for (int j=0; j<Constants.SIZE_4; j++) {
				try {
					r.set(i,j, this.array[i][j]+B.get(i,j));
				} catch (IndiceOutOfBoundException e) {
					// Do nothing, this won't happen as all arrays are controlled in size (coming from Vector4 and Matrix4)
					if (Tracer.error) Tracer.traceError(this.getClass(), "Unexpected exception: "+e);
					e.printStackTrace();				
				}
			}
		}
		return r;		
	}

	/**
	 * Matrix addition A=A+B. This Matrix (A) is modified and contains the result of the operation.
	 * @param B the Matrix to be added to this Matrix
	 */
	public void plusEquals(Matrix4 B) {
		for (int i=0; i<Constants.SIZE_4; i++) {
			for (int j=0; j<Constants.SIZE_4; j++) {
				this.array[i][j] = this.array[i][j]+B.get(i,j);
			}
		}
	}

	/**
	 * Matrix subtraction C=A-B. Do not modify this Matrix (A), this Matrix and return C a newly created Matrix.
	 * @param B the Matrix to be subtracted
	 * @return C, a new Matrix, subtraction of this Matrix (A) and B Matrix
	 */
	public Matrix4 minus(Matrix4 B) {
		Matrix4 r = new Matrix4();
		for (int i=0; i<Constants.SIZE_4; i++) {
			for (int j=0; j<Constants.SIZE_4; j++) {
				try {
					r.set(i,j, this.array[i][j]-B.get(i,j));
				} catch (IndiceOutOfBoundException e) {
					// Do nothing, this won't happen as all arrays are controlled in size (coming from Vector4 and Matrix4)
					if (Tracer.error) Tracer.traceError(this.getClass(), "Unexpected exception: "+e);
					e.printStackTrace();				
				}
			}
		}
		return r;		
	}
	
	/**
	 * Matrix subtraction A=A-B. This Matrix (A) is modified and contains the result of the operation.
	 * @param B the Matrix to be subtracted to this Matrix
	 */
	/**
	 * @param B
	 */
	public void minusEquals(Matrix4 B) {
		for (int i=0; i<Constants.SIZE_4; i++) {
			for (int j=0; j<Constants.SIZE_4; j++) {
				this.array[i][j] = this.array[i][j]-B.get(i,j);
			}
		}
	}

	public Vector4 times(Vector4 v) {
		// Rely on the service provided by the Vector3D class
		// Optimal call is to use Vector3D method directly
		return v.times(this);
	}
	
	/**
	 * Provide the subMatrix 3x3 of the matrix 4x4 (first 3 lines and 3 columns)
	 * @return a new Matrix corresponding to the subMatrix 3x3
	 */
	public Matrix3 getMatrix3() {
		Matrix3 r = new Matrix3();
		for (int i=0; i<Constants.SIZE_3; i++) {
			for (int j=0; j<Constants.SIZE_3; j++) {
				try {
					r.set(i,j,this.array[i][j]);
				} catch (IndiceOutOfBoundException e) {
					// Do nothing, this won't happen as all arrays are controlled in size
					if (Tracer.error) Tracer.traceError(this.getClass(), "Unexpected exception: "+e);
					e.printStackTrace();
				}
			}
		}
		return r;
	}
	
	public Matrix4 inverse() throws NotInvertibleMatrixException {
		Matrix4 identity = new Matrix4(IDENTITY);
		Matrix4 matrix = new Matrix4(this); // copy of the current Matrix to not modify it
		
		/* Methode du Pivot de Gauss

		L’algorithme du pivot de Gauss se déroule de la façon suivante :
		— on choisit un pivot non nul dans la première colonne, disons sur la ligne i;
		— on échange la première et la i-ème ligne ;
		— on effectue les opérations suivantes sur les lignes 2 ≤ k ≤ n : Lk ← Lk − (ak1/a11) * L1 ;
		— on passe à la colonne suivante ;
		À la colonne j, on effectue les opérations suivantes :
		— on choisit un pivot non nul dans la colonne j dans une ligne d’indice supérieur (≥) à j ;
		— on ramène le pivot sur la ligne j en effectuant éventuellement un échange de lignes ;
		— on effectue les opérations suivantes sur les lignes j < k ≤ n : Lk ← Lk − (akj/ajj) * Lj.
		
		Ces opérations ont pour effet de transformer le système en un système triangulaire. Une fois arrivé à un système
		triangulaire de la forme :
			a∗11.u1  + 	a∗12.u2  + · · · +	a∗1n.un = b∗1
						a∗22.u2  + · · · +	a∗2n.un = b∗2
						.
							.
								.
											a∗n.nun = b∗n
		il suffit d’effectuer les opérations suivantes :
		
			un = b∗n/ a∗nn										(1)
			un−1 = (1 / a∗n−1,n−1) * (b∗n−1 − a∗n−1,nun)			(2)
				.
				.
				. 												(3)
			u1 = (1 / a∗11) * (b∗1 − a∗12u2 − · · · − a∗1nun)	(4)

		 */
		
		//methode du pivot de gauss
		int r = -1; //indiceLigneDernierPivot

		try {
			// parcours de toutes les colonnes
			for (int j = 0; j < Constants.SIZE_4; j++) { // colonne
				int k = indiceOfMaxColonne(this, j); // maxColonne
				float pivot = matrix.get(k,j);
				if (pivot != 0) {
					r++;
					// Change ligne k et r
					float tmpPivot;
					if (k != r) {
						for (int i = 0; i < Constants.SIZE_4; i++) {
							tmpPivot = matrix.get(k,i);
							matrix.set(k,i,matrix.get(r,i));
							matrix.set(r,i, tmpPivot);
							tmpPivot = identity.get(k,i);
							identity.set(k,i, identity.get(r,i));
							identity.set(r,i, tmpPivot);
						}
						// division de la ligne k par pivot
						for (int jTmp = 0; jTmp < Constants.SIZE_4; jTmp++) {
							matrix.set(k, jTmp, matrix.get(k, jTmp)/ pivot);
							identity.set(k, jTmp, identity.get(k, jTmp)/pivot);
						}
					}
					for (int i = 0; i < Constants.SIZE_4; i++) {
						if (i != r) {
							// soustraire la ligne i à la ligne r multiplié par matrice[i, j]
							for (int tmpColonne = 0; tmpColonne < Constants.SIZE_4; tmpColonne++) {
								matrix.set(i,tmpColonne, matrix.get(i,tmpColonne) - matrix.get(r,tmpColonne) * matrix.get(i,j));
								identity.set(i,tmpColonne,  identity.get(i,tmpColonne) - identity.get(r,tmpColonne) * matrix.get(i,j));
							}
						}
					}
				} else { // pivot is null
					throw new NotInvertibleMatrixException();
				}
			}
		} catch (IndiceOutOfBoundException e) {
			// Do nothing, this won't happen as all arrays are controlled in size
			if (Tracer.error) Tracer.traceError(this.getClass(), "Unexpected exception: "+e);
			e.printStackTrace();
		}
		return identity;
	}
	
	protected int indiceOfMaxColonne(Matrix4 m, int colonne) {
        double max = m.get(0, colonne);
        int indiceMax = 0;
        for (int i=1; i < Constants.SIZE_4; i++) {
            if (max < Math.abs(m.get(i, colonne))) {
                max = Math.abs(m.get(i,colonne));
                indiceMax = i;
            }
        }
        return indiceMax;
}

	
}
