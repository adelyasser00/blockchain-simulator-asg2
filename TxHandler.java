//“I acknowledge that I am aware of the academic integrity guidelines of this course, and that I worked on this assignment independently without any unauthorized help with coding or testing.” - Adel Yasser Yassin 
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
        int checkCount=0;
        // (1)
        boolean check = false;
        // obtain all the UTXO currently in the pool
        ArrayList<UTXO> listOfUTXO = utxoPool.getAllUTXO();
        // check if this transaction is in the pool
        for(int i=0; i< listOfUTXO.size(); i++){
            if(listOfUTXO.get(i).equals(tx.getHash())){
                checkCount=checkCount+1;
                break;
            }
        }
        if(checkCount==0){
            return false;
        }
        else{
            // loop through all the inputs of the given transaction
            for(int i=0; i<tx.numInputs(); i++){
                // obtain a temporary input for the transaction
                Transaction.Input tempTxInput = tx.getInput(i);
                // create a temporary UTXO for the input to be checked against the pool
                UTXO tempUtxo = new UTXO(tempTxInput.prevTxHash, tempTxInput.outputIndex);
                // check if the UTXO is in the pool beforehand
                if(!listOfUTXO.contains(tempUtxo)){
                    return false;
                }
                else{
                    // (2)
                    // store the output of the UTXO in a temporary output for signature checking
                    Transaction.Output tempUtxoOutput = utxoPool.getTxOutput(tempUtxo);
                    // check if the signature is valid using the provided method.
                    if(!Crypto.verifySignature(tempUtxoOutput.address, tx.getRawDataToSign(i), tempTxInput.signature)){
                        return false;
                    }
                    // (3)
                    else{
                        // check if the UTXO is claimed multiple times by the transaction
                        for(int j=i+1; j<tx.numInputs(); j++){
                            Transaction.Input checkedTxInput = tx.getInput(j);
                            // 
                            UTXO checkedUtxo = new UTXO(checkedTxInput.prevTxHash, checkedTxInput.outputIndex);

                            if(tempUtxo.equals(checkedUtxo)){
                                return false;
                            }
                        }
                    }
                }
            }
            // (4)
            // check if all the outputs are non-negative
            for(int i=0; i<tx.numOutputs(); i++){
                Transaction.Output checkedOutput = tx.getOutput(i);
                if(checkedOutput.value<0){
                    return false;
                }
            }
            // (5)
            double sumInputs=0, sumOutputs=0;
            // loop through all the inputs of the transaction and sum their values
            for(int i=0; i<tx.numInputs(); i++){
                // obtain the input of the transaction currently checked
                Transaction.Input input = tx.getInput(i);
                // create a UTXO for it
                UTXO checkedUtxo = new UTXO(input.prevTxHash, input.outputIndex);
                Transaction.Output output = utxoPool.getTxOutput(checkedUtxo);
                sumInputs=sumInputs+output.value;
            }
            // loop through all the outputs of the transaction and sum their values
            for(int i=0; i<tx.numOutputs(); i++){
                Transaction.Output output = tx.getOutput(i);
                sumOutputs=sumOutputs+output.value;
            }
            // check if the sum of inputs is greater than or equal to the sum of outputs
            if(sumInputs<sumOutputs){
                return false;
            }
            else{
                return true;
            }
        }


    }

    /**
     * Handles each epoch by receiving an unordered array of proposed transactions, checking each
     * transaction for correctness, returning a mutually valid array of accepted transactions, and
     * updating the current UTXO pool as appropriate.
     */
    public Transaction[] handleTxs(Transaction[] possibleTxs) {
        // IMPLEMENT THIS
        // TODO: check if unordered transactions depend on each other
        // create an array list to store the accepted transactions
        ArrayList<Transaction> acceptedTxs = new ArrayList<Transaction>();
        for(int i=0; i<possibleTxs.length; i++){
            if(isValidTx(possibleTxs[i])){
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
