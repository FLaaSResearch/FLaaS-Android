package org.sensingkit.flaas.model;

import com.google.gson.annotations.SerializedName;

import java.util.Date;

public class RetroProject {

    @SerializedName("id")
    private Integer id;

    @SerializedName("create_date")
    private Date createDate;

    @SerializedName("title")
    private String title;

    @SerializedName("description")
    private String description;

    @SerializedName("status")
    private String status;

    @SerializedName("model")
    private String model;

    @SerializedName("dataset")
    private String dataset;

    @SerializedName("dataset_type")
    private String datasetType;

    @SerializedName("training_mode")
    private String trainingMode;

    @SerializedName("number_of_rounds")
    private Integer numberOfRounds;

    @SerializedName("number_of_apps")
    private Integer numberOfApps;

    @SerializedName("number_of_samples")
    private Integer numberOfSamples;

    @SerializedName("number_of_epochs")
    private Integer numberOfEpochs;

    @SerializedName("seed")
    private Integer seed;

    @SerializedName("current_round")
    private Integer currentRound;

    public RetroProject(Integer id, Date createDate, String title, String description, String status, String model, String dataset, String datasetType, String trainingMode, Integer numberOfRounds, Integer numberOfApps, Integer numberOfSamples, Integer numberOfEpochs, Integer seed, Integer currentRound) {

        this.id = id;
        this.createDate = createDate;
        this.title = title;
        this.description = description;
        this.status = status;

        this.model = model;
        this.dataset = dataset;
        this.datasetType = datasetType;
        this.trainingMode = trainingMode;
        this.numberOfRounds = numberOfRounds;
        this.numberOfApps = numberOfApps;
        this.numberOfSamples = numberOfSamples;
        this.numberOfEpochs = numberOfEpochs;
        this.seed = seed;

        this.currentRound = currentRound;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Date getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Date createDate) {
        this.createDate = createDate;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
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

    public Integer getNumberOfRounds() {
        return numberOfRounds;
    }

    public void setNumberOfRounds(Integer numberOfRounds) {
        this.numberOfRounds = numberOfRounds;
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

    public Integer getCurrentRound() {
        return currentRound;
    }

    public void setCurrentRound(Integer currentRound) {
        this.currentRound = currentRound;
    }

    public String getDatasetType() {
        return datasetType;
    }

    public void setDatasetType(String datasetType) {
        this.datasetType = datasetType;
    }

    public String getTrainingMode() {
        return trainingMode;
    }

    public void setTrainingMode(String trainingMode) {
        this.trainingMode = trainingMode;
    }
}
