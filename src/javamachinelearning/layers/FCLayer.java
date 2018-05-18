package javamachinelearning.layers;

import java.nio.ByteBuffer;

import javamachinelearning.optimizers.Optimizer;
import javamachinelearning.regularizers.Regularizer;
import javamachinelearning.utils.Activation;
import javamachinelearning.utils.Tensor;

public class FCLayer implements ParamsLayer{
	private Tensor weights;
	private Tensor deltaWeights;
	private Tensor bias;
	private Tensor deltaBias;
	
	private Activation activation;
	private int prevSize;
	private int nextSize;
	private int changeCount;
	private boolean alreadyInit = false;
	private boolean useBias = true;
	
	public FCLayer(int nextSize, Activation activation){
		this.nextSize = nextSize;
		this.activation = activation;
	}
	
	@Override
	public int[] nextShape(){
		return new int[]{nextSize};
	}
	
	@Override
	public int[] prevShape(){
		return new int[]{prevSize};
	}

	@Override
	public void init(int[] prevSize){
		this.prevSize = prevSize[0];
		if(!alreadyInit){
			this.weights = new Tensor(new int[]{prevSize[0], nextSize}, true);
			if(useBias)
				this.bias = new Tensor(new int[]{nextSize}, false);
		}
		this.deltaWeights = new Tensor(new int[]{prevSize[0], nextSize}, false);
		if(useBias)
			this.deltaBias = new Tensor(new int[]{nextSize}, false);
	}
	
	@Override
	public ParamsLayer withParams(Tensor w, Tensor b){
		weights = w;
		if(useBias)
			bias = b;
		alreadyInit = true;
		return this;
	}
	
	@Override
	public ParamsLayer noBias(){
		useBias = false;
		return this;
	}
	
	@Override
	public Tensor bias(){
		return bias;
	}
	
	@Override
	public Tensor weights(){
		return weights;
	}
	
	@Override
	public void setBias(Tensor b){
		if(useBias)
			bias = b;
	}
	
	@Override
	public void setWeights(Tensor w){
		weights = w;
	}
	
	@Override
	public Tensor forwardPropagate(Tensor input, boolean training){
		Tensor x = weights.dot(input);
		if(useBias)
			x = x.add(bias);
		return activation.activate(x);
	}
	
	@Override
	public Tensor backPropagate(Tensor prevRes, Tensor nextRes, Tensor error, Optimizer optimizer, Regularizer regularizer, int l){
		// error wrt layer output derivative
		Tensor grads = error.mul(activation.derivative(nextRes));
		
		// error wrt weight derivative
		if(regularizer == null){
			deltaWeights = deltaWeights.sub(optimizer.optimizeWeight(prevRes.mulEach(grads), l));
		}else{ // also add the regularization derivative if necessary
			deltaWeights = deltaWeights.sub(optimizer.optimizeWeight(
					prevRes.mulEach(grads).add(regularizer.derivative(weights)), l));
		}
		
		// error wrt bias derivative
		// not multiplied by prev outputs!
		if(useBias)
			deltaBias = deltaBias.sub(optimizer.optimizeBias(grads, l));
		
		// new error should be affected by weights
		Tensor nextError = weights.T().dot(grads);
		
		changeCount++;
		
		return nextError;
	}
	
	@Override
	public void update(){
		// handles postponed updates, by average updating values
		weights = weights.add(deltaWeights.div(Math.max(changeCount, 1)));
		deltaWeights = new Tensor(deltaWeights.shape(), false);
		if(useBias){
			bias = bias.add(deltaBias.div(Math.max(changeCount, 1)));
			deltaBias = new Tensor(deltaBias.shape(), false);
		}
		changeCount = 0;
	}
	
	@Override
	public Activation getActivation(){
		return activation;
	}
	
	@Override
	public int byteSize(){
		// 8 bytes for each double
		return Double.BYTES * weights.size() + (useBias ? Double.BYTES * bias.size() : 0);
	}
	
	@Override
	public ByteBuffer bytes(){
		ByteBuffer bb = ByteBuffer.allocate(byteSize());
		for(int i = 0; i < weights.size(); i++){
			bb.putDouble(weights.flatGet(i));
		}
		if(useBias){
			for(int i = 0; i < bias.size(); i++){
				bb.putDouble(bias.flatGet(i));
			}
		}
		bb.flip();
		return bb;
	}
	
	@Override
	public void readBytes(ByteBuffer bb){
		double[] w = new double[weights.size()];
		for(int i = 0; i < w.length; i++){
			w[i] = bb.getDouble();
		}
		weights = new Tensor(weights.shape(), w);
		
		if(useBias){
			double[] b = new double[bias.size()];
			for(int i = 0; i < b.length; i++){
				b[i] = bb.getDouble();
			}
			bias = new Tensor(bias.shape(), b);
		}
	}
}
