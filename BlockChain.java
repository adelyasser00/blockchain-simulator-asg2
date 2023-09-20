// "I acknowledge that I am aware of the academic integrity guidelines of this course, and that I worked on this assignment independently without any unauthorized help with coding or testing."- Adel Yasser Yassin
// The BlockChain class should maintain only limited block nodes to satisfy the functionality.
// You should not have all the blocks added to the block chain in memory 
// as it would cause a memory overflow.
import java.util.ArrayList;
import java.util.HashMap;

public class BlockChain {
    public static final int CUT_OFF_AGE = 10;
    
    TransactionPool bcTxPool;
    BlockClass bcMaxHeightBlock;
    HashMap <ByteArrayWrapper, BlockClass> bcBlockMap;
    // The intention of this class is to make it easier to manipulate the blocks in the blockchain
     class BlockClass {
        Block block;
        BlockClass parentBlock;
        UTXOPool utxoPool;
        int height;

        BlockClass(Block block,BlockClass parentBlock,UTXOPool utxoPool){
            this.block=block;
            this.parentBlock=parentBlock;
            this.utxoPool = new UTXOPool(utxoPool);
            if(parentBlock!=null){
                this.height = parentBlock.getHeight()+1;
            }
            else{
                this.height = 1;
            }

        }

        Block getBlock() {
            return block;
        }
        BlockClass getParentBlock() {
            return parentBlock;
        }

        UTXOPool getUtxoPoolCopy() {
            return new UTXOPool(utxoPool);
        }

        int getHeight() {
            return height;
        }

    }

    /**
     * create an empty blockchain with just a genesis block. Assume {@code genesisBlock} is a valid
     * block
     */
    public BlockChain(Block genesisBlock) {
        // IMPLEMENT THIS
        UTXOPool utxoPool = new UTXOPool();
        Transaction coinBase = genesisBlock.getCoinbase();
        // add all the outputs of the coinbase transaction to the utxoPool
        for(int i=0; i<coinBase.numOutputs(); i++){
            UTXO tempUtxo = new UTXO(coinBase.getHash(), i);
            Transaction.Output tempTxOutput = coinBase.getOutput(i);
            
            utxoPool.addUTXO(tempUtxo, tempTxOutput);
        }
        // create a new block class for the genesis block for easy manipulation
        BlockClass genesisBlockClass = new BlockClass(genesisBlock, null, utxoPool);
        // create a new hashmap to store the blocks in the blockchain
        // the key is the hash of the block and the value is the block class
        // ByteArrayWrapper is used to make the hash of the block as the key
        bcBlockMap = new HashMap<ByteArrayWrapper, BlockClass>();
        // add the genesis block to the blockchain
        bcBlockMap.put(new ByteArrayWrapper(genesisBlock.getHash()), genesisBlockClass);
        // set the max height block to the genesis block
        bcMaxHeightBlock = genesisBlockClass;
        // create a new transaction pool for the blockchain which is global
        bcTxPool = new TransactionPool();
    }

    /** Get the maximum height block */
    public Block getMaxHeightBlock() {
        // IMPLEMENT THIS

        return bcMaxHeightBlock.getBlock();
    }

    /** Get the UTXOPool for mining a new block on top of max height block */
    public UTXOPool getMaxHeightUTXOPool() {
        // IMPLEMENT THIS

        return bcMaxHeightBlock.getUtxoPoolCopy();
    }

    /** Get the transaction pool to mine a new block */
    public TransactionPool getTransactionPool() {
        // IMPLEMENT THIS
        
        return bcTxPool;
    }

    /**
     * Add {@code block} to the blockchain if it is valid. For validity, all transactions should be
     * valid and block should be at {@code height > (maxHeight - CUT_OFF_AGE)}, where maxHeight is 
     * the current height of the blockchain.
	 * <p>
	 * Assume the Genesis block is at height 1.
     * For example, you can try creating a new block over the genesis block (i.e. create a block at 
	 * height 2) if the current blockchain height is less than or equal to CUT_OFF_AGE + 1. As soon as
	 * the current blockchain height exceeds CUT_OFF_AGE + 1, you cannot create a new block at height 2.
     * maximal set of valid transactions.
     * if maximum set is not the entire set
     * if possibleTx!=acceptedTx
     * block rejected
     * @return true if block is successfully added
     */
    public boolean addBlock(Block block) {
        // IMPLEMENT THIS
        // control the maximum height
        // when max height changes
        //TODO: check that ALL transactions can be added to the pool
        // HandleTxs : return array of transactions
        // compare length of the returned to the original
        
        // we must test the validity before adding the block to the blockchain or creating it.
        // get the parent block
        byte[] parentBlockHash = block.getPrevBlockHash();
        // check if the parent block still exists
        if(!bcBlockMap.containsKey(new ByteArrayWrapper(parentBlockHash))){
            return false;
        }
        // check if the transactions are valid
        ArrayList<Transaction> blockTxs = block.getTransactions();
        // convert the arraylist to an array for the handleTxs method
        Transaction[] blockTxsArray = blockTxs.toArray(new Transaction[blockTxs.size()]);
        // create a new transaction handler with a copy utxo pool of the parent block
        TxHandler TxHandler = new TxHandler(bcMaxHeightBlock.getUtxoPoolCopy());
        // get the accepted transactions using the handleTxs method
        Transaction[] acceptedTxs= TxHandler.handleTxs(blockTxsArray);
        // check if the accepted transactions are the same as the original transactions
        if(acceptedTxs.length!=blockTxsArray.length){
            return false;
        }

        // get the parent block class
        BlockClass parentBlockClass = bcBlockMap.get(new ByteArrayWrapper(parentBlockHash));
        // get the parent block height
        int parentBlockHeight = parentBlockClass.getHeight();
        // check if the block height is valid
        if(parentBlockHeight + 1 <= bcMaxHeightBlock.getHeight() - CUT_OFF_AGE){
            return false;
            
        }
        // beyond this point, the block is valid and can be added to the blockchain

        // create a UTXOPool for the block locally
        UTXOPool blockUtxoPool = new UTXOPool();
        
        // if there are any transactions in the block, add them to the utxo pool
        if(blockTxs.size()!=0 ){

            for(Transaction tx: blockTxs){
                for(int i=0; i<tx.numOutputs(); i++ ){
                    // get the output and store it in a utxo and add it to the utxo pool
                    Transaction.Output tempTxOutput = tx.getOutput(i);
                    UTXO tempUtxo = new UTXO(tx.getHash(), i);
                    
                    blockUtxoPool.addUTXO(tempUtxo, tempTxOutput);
                }

            for (int i=0; i<tx.numInputs(); i++){
                // remove the inputs similarly from the utxo pool as they are spent
                Transaction.Input tempTxInput = tx.getInput(i);
                UTXO tempUtxo = new UTXO(tempTxInput.prevTxHash, tempTxInput.outputIndex);

                blockUtxoPool.removeUTXO(tempUtxo);
                }
            }
        }
        // add the coinbase transaction to the current utxo pool if any exists
        Transaction coinBase = block.getCoinbase();
        for(int i=0; i<coinBase.numOutputs(); i++){
            UTXO tempUtxo = new UTXO(coinBase.getHash(), i);
            Transaction.Output tempTxOutput = coinBase.getOutput(i);
            
            blockUtxoPool.addUTXO(tempUtxo, tempTxOutput);
        }
        // create the block node object (BlockClass) 
        BlockClass blockClass = new BlockClass(block, parentBlockClass, blockUtxoPool);
        // add the block to the blockchain
        bcBlockMap.put(new ByteArrayWrapper(block.getHash()), blockClass);
        // check if the new block is the new max height block
        if(blockClass.getHeight() > bcMaxHeightBlock.getHeight()){
            // set the new max height block
            bcMaxHeightBlock = blockClass;
            // check if new max height block is older than the cut off age
            // if so, we delete all references to nodes younger than the cut off age
            // so that the garbage collector can delete them
            if(bcMaxHeightBlock.height > CUT_OFF_AGE){
                BlockClass targetBlockClass= blockClass;
                for(int i=0; i<CUT_OFF_AGE-2; i++){
                    targetBlockClass = targetBlockClass.getParentBlock();
                }
                BlockClass parentTargetBlockClass = targetBlockClass.getParentBlock();
                bcBlockMap.remove(new ByteArrayWrapper(parentTargetBlockClass.getBlock().getHash()));
                parentTargetBlockClass=null;
                targetBlockClass.parentBlock=null;
            }
        }
        
        // reaching this point means that the block was successfully added to the blockchain
        return true;

    

    }

    /** Add a transaction to the transaction pool */
    public void addTransaction(Transaction tx) {
        // IMPLEMENT THIS
        bcTxPool.addTransaction(tx);
    }
}