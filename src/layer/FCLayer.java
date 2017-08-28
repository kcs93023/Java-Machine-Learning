package layer;

import java.nio.ByteBuffer;

import edge.Edge;
import optimizer.Optimizer;
import optimizer.Update;
import utils.Activation;

public class FCLayer implements Layer{
	private Edge[] edges;
	private Activation activation;
	private int prevSize;
	private int nextSize;
	private double[] bias;
	private double[] deltaBias;
	private double dropout;
	
	public FCLayer(int nextSize){
		this.nextSize = nextSize;
		this.activation = Activation.linear;
		this.bias = new double[nextSize];
		this.deltaBias = new double[nextSize];
		this.dropout = 0.0;
	}
	
	public FCLayer(int nextSize, Activation activation){
		this.nextSize = nextSize;
		this.activation = activation;
		this.bias = new double[nextSize];
		this.deltaBias = new double[nextSize];
		this.dropout = 0.0;
	}
	
	public FCLayer(int nextSize, Activation activation, double dropout){
		this.nextSize = nextSize;
		this.activation = activation;
		this.bias = new double[nextSize];
		this.deltaBias = new double[nextSize];
		this.dropout = dropout;
	}
	
	@Override
	public int nextSize(){
		return nextSize;
	}
	
	@Override
	public int prevSize(){
		return prevSize;
	}

	@Override
	public void init(int prevSize){
		this.prevSize = prevSize;
		edges = new Edge[prevSize * nextSize];
		for(int i = 0; i < prevSize; i++){
			for(int j = 0; j < nextSize; j++){
				edges[i * nextSize + j] = new Edge(i, j, prevSize);
			}
		}
	}
	
	@Override
	public void init(int prevSize, double[][] weights, double[] bias){
		this.prevSize = prevSize;
		edges = new Edge[prevSize * nextSize];
		for(int i = 0; i < prevSize; i++){
			for(int j = 0; j < nextSize; j++){
				edges[i * nextSize + j] = new Edge(i, j, weights[i][j]);
			}
		}
		this.bias = bias;
	}
	
	@Override
	public double[] getBias(){
		return bias;
	}
	
	@Override
	public Edge[] edges(){
		return edges;
	}
	
	@Override
	public double[] forwardPropagate(double[] input){
		double[] result = new double[nextSize];
		for(int i = 0; i < edges.length; i++){
			result[edges[i].getNodeB()] += input[edges[i].getNodeA()] * edges[i].getWeight();
		}
		for(int i = 0; i < result.length; i++){
			result[i] += bias[i];
		}
		for(int i = 0; i < result.length; i++){
			result[i] = activation.activate(result[i], result);
		}
		return result;
	}
	
	@Override
	public Update backPropagate(int l, double[] prevResult, double[] nextResult, double[] error, double[] error2, double lambda, double weightSum, Optimizer optimizer, int size, int max, int max2){
		double[] newError = new double[prevSize()];
		double[] newError2 = new double[prevSize()];
		for(int i = 0; i < edges.length; i++){
			Edge e = edges[i];
			e.addWeight(optimizer.optimizeWeight(l, e, prevResult, nextResult, error, lambda, weightSum, activation, size, max, nextSize));
			newError[e.getNodeA()] += e.getWeight() * (error[e.getNodeB()] + lambda * weightSum) * activation.derivative(nextResult[e.getNodeB()]);
			newError2[e.getNodeA()] += e.getWeight() * error2[e.getNodeB()] * activation.derivative(nextResult[e.getNodeB()]);
		}
		for(int i = 0; i < nextSize(); i++){
			deltaBias[i] += optimizer.optimizeBias(l, i, nextResult, error2, activation, size, max2);
		}
		return new Update(newError, newError2);
	}
	
	@Override
	public void update(){
		for(int i = 0; i < edges.length; i++){
			edges[i].update();
		}
		for(int i = 0; i < nextSize(); i++){
			bias[i] += deltaBias[i];
			deltaBias[i] = 0.0;
		}
	}
	
	@Override
	public Activation getActivation(){
		return activation;
	}
	
	@Override
	public double getDropout(){
		return dropout;
	}
	
	@Override
	public int byteSize(){
		return 8 * edges.length + 8 * bias.length;
	}
	
	@Override
	public ByteBuffer toBytes(){
		ByteBuffer bb = ByteBuffer.allocate(byteSize());
		for(int i = 0; i < edges.length; i++){
			bb.putDouble(edges[i].getWeight());
		}
		for(int i = 0; i < bias.length; i++){
			bb.putDouble(bias[i]);
		}
		bb.flip();
		return bb;
	}
}