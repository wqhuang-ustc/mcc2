#!/bin/bash
sudo kubectl delete -f mongo-svc-a.yaml
sudo kubectl delete -f mongo-svc-b.yaml
sudo kubectl delete -f mongo-svc-c.yaml
sudo kubectl delete -f mongo-rc1.yaml
sudo kubectl delete -f mongo-rc2.yaml
sudo kubectl delete -f mongo-rc3.yaml
sudo kubectl delete -f ocr-svc-a.yaml
sudo kubectl delete -f ocr-rc1.yaml

