//"I acknowledge that I am aware of the academic integrity guidelines of this course, and that I worked on this assignment independently without any unauthorized help with coding or testing." - Adel Yasser Yassin 
import java.util.ArrayList;

public class TxHandler {

    private UTXOPool utxoPool;

    /**
     * Creates a public ledger whose current UTXOPool (collection of unspent transaction outputs) is
     * {@code utxoPool}. 
     */
    public TxHandler(UTXOPool utxoPool) {
        this.utxoPool = new UTXOPool(utxoPool);
    }

    /**
     * @return true if:
     * (1) all outputs claimed by {@code tx} are in the current UTXO pool, 
     * (2) the signatures on each input of {@code tx} are valid, 
     * (3) no UTXO is claimed multiple times by {@code tx},
     * (4) all of {@code tx}s output values are non-negative, and
     * (5) the sum of {@code tx}s input values is greater than or equal to the sum of its output
     *     values; and false otherwise.
     */
    public boolean isValidTx(Transaction tx) {
        // IMPLEMENT THIS
        // (1) all outputs claimed by {@code tx} are in the current UTXO pool,
        // check if each transaction input is in the pool
        for(int i=0; i<tx.numInputs(); i++){
            Transaction.Input tempTxInput = tx.getInput(i);
            // make it into UTXO object to check if it is in the pool
            UTXO tempUtxo = new UTXO(tempTxInput.prevTxHash, tempTxInput.outputIndex);
            if(!utxoPool.contains(tempUtxo)){
                return false;
            }
            else{
                // (2) the signatures on each input of {@code tx} are valid, 
                Transaction.Output tempUtxoOutput = utxoPool.getTxOutput(tempUtxo);
                // signature check done as referenced in the tutorial notes
                if(!Crypto.verifySignature(tempUtxoOutput.address, tx.getRawDataToSign(i), tempTxInput.signature)){
                    return false;
                }
                // (3) no UTXO is claimed multiple times by {@code tx},
                else{
                    for(int x=i+1; x<tx.numInputs(); x++){
                        // check if the UTXO is claimed multiple times
                        Transaction.Input checkedTxInput = tx.getInput(x);
                        // make utxo object each time to check if it is in the pool
                        UTXO checkedUtxo = new UTXO(checkedTxInput.prevTxHash, checkedTxInput.outputIndex);

                        if(tempUtxo.equals(checkedUtxo)){
                            return false;
                        }
                    }
                }
            }
        }
        // (4) all of {@code tx}s output values are non-negative,
        for(int i=0; i<tx.numOutputs(); i++){
            if(tx.getOutput(i).value < 0){
                return false;
            }
        }
        // (5) the sum of {@code tx}s input values is greater than or equal to the sum of its output
        // values; and false otherwise.
        double totalInputs = 0, totalOutputs = 0;
        for(int i=0; i<tx.numInputs(); i++){
            // obtain a temporary input for the transaction and create a temporary UTXO for the input to be summed up
            Transaction.Input tempTxInput = tx.getInput(i);
            UTXO tempUtxo = new UTXO(tempTxInput.prevTxHash, tempTxInput.outputIndex);
            totalInputs = totalInputs + utxoPool.getTxOutput(tempUtxo).value;
        }
        for(int i=0; i<tx.numOutputs(); i++){
            totalOutputs += tx.getOutput(i).value;
        }
        // check if the sum of inputs is greater than or equal to the sum of outputs
        if(totalInputs < totalOutputs){
            return false;
        }
        // if all the previous is true, then the transaction is valid
        return true;
    }


    /**
     * Handles each epoch by receiving an unordered array of proposed transactions, checking each
     * transaction for correctness, returning a mutually valid array of accepted transactions, and
     * updating the current UTXO pool as appropriate.
     */
    public Transaction[] handleTxs(Transaction[] possibleTxs) {
        // IMPLEMENT THIS
        // TODO: check if unordered transactions depend on each other then reorder them
        
        // loop through every possible transaction
        for(int i=0; i<possibleTxs.length; i++){
            int flag=0;
            Transaction currentTx = possibleTxs[i];
            // loop through every input of the current transaction
            for(int j=0; j< currentTx.numInputs()-1; j++){
                Transaction.Input currentInput = currentTx.getInput(j);
                Transaction checkedTx = possibleTxs[i+1];
                // loop through every output of the checked transaction against the current transaction
                // input
                for (int x=0; x< checkedTx.numOutputs(); x++){
                    // check if the current input is equal to the checked output
                    if(currentInput.prevTxHash.equals(checkedTx.getHash()) && currentInput.outputIndex==x){
                        // if the current input is equal to the checked output, swap the transactions
                        Transaction tempTx = possibleTxs[i];
                        possibleTxs[i] = possibleTxs[i+1];
                        possibleTxs[i+1] = tempTx;
                        // reset the counters and restart from scratch. more efficient code will be added later.
                        i=0;
                        j=0;
                        flag=1;
                        break;
                    }
                }
                if ( flag == 1){
                    break;
                }
            }
        }
        // create an array list to store the accepted transactions
        ArrayList<Transaction> acceptedTxs = new ArrayList<Transaction>();
        for(int i=0; i<possibleTxs.length; i++){
            if(isValidTx(possibleTxs[i])){
                // add the transaction to the accepted transactions
                acceptedTxs.add(possibleTxs[i]);
                // remove the inputs from the utxo pool
                for(int j=0; j<possibleTxs[i].numInputs(); j++){
                    // obtain the input of the transaction currently checked and store in utxo object to remove
                    Transaction.Input checkedInput = possibleTxs[i].getInput(j);
                    UTXO checkedUtxo = new UTXO(checkedInput.prevTxHash, checkedInput.outputIndex);
                    utxoPool.removeUTXO(checkedUtxo);
                }
                // add the outputs to the utxo pool
                for(int j=0; j<possibleTxs[i].numOutputs(); j++){
                    // obtain the output of the transaction currently checked and store in utxo object to add
                    Transaction.Output checkedOutput = possibleTxs[i].getOutput(j);
                    UTXO checkedUtxo = new UTXO(possibleTxs[i].getHash(), j);
                    utxoPool.addUTXO(checkedUtxo, checkedOutput);
                }
            }
        }

        // convert from array list to array to return
        Transaction[] acceptedTxsArray = new Transaction[acceptedTxs.size()];
        for(int i=0; i<acceptedTxs.size(); i++){
            acceptedTxsArray[i]=acceptedTxs.get(i);
        }
        // return the accepted transactions after all operations are done
        return acceptedTxsArray;
    }

}
