sudo gcloud container clusters create cluster-final
sudo gcloud container clusters get-credentials cluster-final
sudo gcloud compute disks create mongodb-disk7
sudo gcloud compute disks create mongodb-disk8
sudo gcloud compute disks create mongodb-disk9
sudo kubectl create -f kube-mongo-final.yaml
