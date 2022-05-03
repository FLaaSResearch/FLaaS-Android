package org.sensingkit.flaas.model;

import com.google.gson.annotations.SerializedName;

import java.util.Date;

public class RetroRound {

    @SerializedName("id")
    private Integer id;

    @SerializedName("create_date")
    private Date createDate;

    @SerializedName("round_number")
    private Integer roundNumber;

    @SerializedName("status")
    private Integer status;

    @SerializedName("start_training_date")
    private Date startTrainingDate;

    @SerializedName("stop_training_date")
    private Date stopTrainingDate;

    @SerializedName("number_of_samples")
    private Integer numberOfSamples;

    @SerializedName("number_of_epochs")
    private Integer numberOfEpochs;

    @SerializedName("seed")
    private Integer seed;

    @SerializedName("model")
    private String model;

    @SerializedName("dataset")
    private String dataset;

    @SerializedName("dataset_type")
    private String datasetType;

    @SerializedName("training_mode")
    private String trainingMode;

    @SerializedName("number_of_apps")
    private Integer numberOfApps;

    public RetroRound(Integer id, Date createDate, Integer roundNumber, Integer status, Date startTrainingDate, Date stopTrainingDate, Integer numberOfSamples, Integer numberOfEpochs, Integer seed, String model, String dataset, String datasetType, String trainingMode, Integer numberOfApps) {

        this.id = id;
        this.createDate = createDate;
        this.roundNumber = roundNumber;
        this.status = status;
        this.startTrainingDate = startTrainingDate;
        this.stopTrainingDate = stopTrainingDate;

        this.numberOfSamples = numberOfSamples;
        this.numberOfEpochs = numberOfEpochs;
        this.seed = seed;
        this.model = model;
        this.dataset = dataset;
        this.datasetType = datasetType;
        this.trainingMode = trainingMode;
        this.numberOfApps = numberOfApps;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getRoundNumber() {
        return roundNumber;
    }

    public void setRoundNumber(Integer roundNumber) {
        this.roundNumber = roundNumber;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public Date getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Date createDate) {
        this.createDate = createDate;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getDataset() {
        return dataset;
    }

    public void setDataset(String dataset) {
        this.dataset = dataset;
    }

    public String getDatasetType() {
        return datasetType;
    }

    public void setDatasetType(String datasetType) {
        this.datasetType = datasetType;
    }

    public Integer getNumberOfApps() {
        return numberOfApps;
    }

    public void setNumberOfApps(Integer numberOfApps) {
        this.numberOfApps = numberOfApps;
    }

    public Integer getNumberOfSamples() {
        return numberOfSamples;
    }

    public void setNumberOfSamples(Integer numberOfSamples) {
        this.numberOfSamples = numberOfSamples;
    }

    public Integer getNumberOfEpochs() {
        return numberOfEpochs;
    }

    public void setNumberOfEpochs(Integer numberOfEpochs) {
        this.numberOfEpochs = numberOfEpochs;
    }

    public Integer getSeed() {
        return seed;
    }

    public void setSeed(Integer seed) {
        this.seed = seed;
    }

    public Date getStartTrainingDate() {
        return startTrainingDate;
    }

    public void setStartTrainingDate(Date startTrainingDate) {
        this.startTrainingDate = startTrainingDate;
    }

    public Date getStopTrainingDate() {
        return stopTrainingDate;
    }

    public void setStopTrainingDate(Date stopTrainingDate) {
        this.stopTrainingDate = stopTrainingDate;
    }

    public String getTrainingMode() {
        return trainingMode;
    }

    public void setTrainingMode(String trainingMode) {
        this.trainingMode = trainingMode;
    }
}
